# Koog Architecture Skill

**Trigger**: Use this skill when designing or implementing AI agent features with Koog.

**vs koog-reference.md**: `koog-reference.md` is a quick API lookup (syntax, imports, snippets).
This skill provides **architecture decisions, patterns, anti-patterns, and testing** — use it first when starting a new Koog feature.

---

## Architecture Decision Tree

```
What do you need?
│
├── Simple Q&A / chat completion
│   └── AIAgent with chatAgentStrategy() or default
│
├── Agent needs external tools (search, DB, API calls)
│   ├── JVM only → annotation-based ToolSet (@Tool)
│   └── Multiplatform → class-based SimpleTool<Args>
│   └── Strategy: chatAgentStrategy()
│
├── Multi-step reasoning (think before acting)
│   └── reActStrategy(reasoningInterval = 1)
│
├── Custom flow / retries / conditional branches
│   └── functionalStrategy { ... } (lambda-based)
│
├── Complex branching with named nodes
│   └── Custom strategy graph: strategy<I,O>("name") { ... }
│
├── Typed structured response (not free-form text)
│   └── requestLLMStructured<T>() + StructureFixingParser
│
├── Long-running agent (many turns, memory matters)
│   └── Add nodeLLMCompressHistory node after tool steps
│
├── Real-time output / streaming UX
│   └── Streaming API → collect Flow<StreamFrame>
│
├── External services via MCP protocol
│   └── McpToolRegistryProvider.fromTransport()
│
└── Multiple cooperating agents
    ├── Agents-as-tools (simpler) → wrap agent in @Tool
    └── A2A protocol (full) → koog-a2a artifact
```

---

## Canonical Patterns

### Agent Setup (DeepSeek — project default)

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

val apiKey = System.getenv("DEEPSEEK_API_KEY")
    ?: error("DEEPSEEK_API_KEY not set")

val agent = AIAgent(
    promptExecutor = SingleLLMPromptExecutor(DeepSeekLLMClient(apiKey)),
    llmModel = DeepSeekModels.DeepSeekChat,
    systemPrompt = "You are a helpful assistant.",
    temperature = 0.7,
    toolRegistry = ToolRegistry { tools(MyTools()) },
    maxIterations = 30
)

val result: String = agent.run("User input here")
```

For reasoning/thinking tasks use `DeepSeekModels.DeepSeekReasoner`.

---

### Tools — Annotation-Based (preferred on JVM)

```kotlin
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.ToolSet

@LLMDescription("Tools for searching and retrieving data")
class SearchTools : ToolSet {

    @Tool
    @LLMDescription("Search for information about a topic")
    fun search(
        @LLMDescription("The search query") query: String,
        @LLMDescription("Maximum number of results (1-10)") limit: Int = 5
    ): String {
        // implementation
        return "results..."
    }

    @Tool
    @LLMDescription("Get details about a specific item by ID")
    fun getDetails(
        @LLMDescription("The item ID") id: String
    ): String {
        return "details..."
    }
}

// Registration
val registry = ToolRegistry { tools(SearchTools()) }

// Merging registries
val combined = registry1 + registry2
```

Rules:
- Every `@Tool` method needs `@LLMDescription`
- Every parameter needs `@LLMDescription`
- Return type must be `String`
- Use simple param types: `String`, `Int`, `Boolean`, `Double`

---

### Tools — Class-Based (multiplatform)

```kotlin
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.Serializable

class CalculateTool : SimpleTool<CalculateTool.Args>() {

    @Serializable
    data class Args(
        val expression: String,
        val precision: Int = 2
    )

    override val descriptor = ToolDescriptor(
        name = "calculate",
        description = "Evaluate a mathematical expression",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "expression",
                description = "Math expression to evaluate (e.g. '2 + 2 * 3')",
                type = ToolParameterType.String
            )
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                name = "precision",
                description = "Decimal places in result",
                type = ToolParameterType.Integer
            )
        )
    )

    override suspend fun execute(args: Args): String {
        // implementation
        return "42"
    }
}
```

---

### Strategies

```kotlin
import ai.koog.agents.core.agent.config.AgentConfig
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.agents.ext.agent.reActStrategy
import ai.koog.agents.core.agent.strategy.functionalStrategy

// Conversational with tool use
val config = AgentConfig(strategy = chatAgentStrategy())

// ReAct: think → act → observe loop
val reactConfig = AgentConfig(strategy = reActStrategy(reasoningInterval = 1))

// Custom lambda flow
val customConfig = AgentConfig(
    strategy = functionalStrategy { input ->
        val step1 = callLLM(input)
        val step2 = callTool("myTool", step1)
        callLLM("Summarize: $step2")
    }
)
```

#### Custom Strategy Graph

```kotlin
import ai.koog.agents.core.agent.strategy.strategy

val myStrategy = strategy<String, String>("my-pipeline") {
    val classify = node { input -> callLLM("Classify: $input") }
    val process  = node { input -> callLLM("Process: $input") }
    val summarize = node { input -> callLLM("Summarize: $input") }

    edge(classify, process) { it.contains("valid") }
    edge(classify, summarize) { !it.contains("valid") }
    edge(process, summarize)

    start(classify)
    finish(summarize)
}

// JVM: visualize the graph for debugging
println(myStrategy.asMermaidDiagram())
```

---

### Structured Output

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.structure.StructureFixingParser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SentimentResult")
@LLMDescription("Sentiment analysis result for a piece of text")
data class SentimentResult(
    @property:LLMDescription("Sentiment label: positive, negative, or neutral")
    val sentiment: String,

    @property:LLMDescription("Confidence score from 0.0 to 1.0")
    val confidence: Double,

    @property:LLMDescription("Brief explanation of the classification")
    val reason: String
)

// Always use StructureFixingParser — never rely on raw LLM JSON
val result: SentimentResult = agent.requestLLMStructured(
    prompt = "Analyze sentiment: 'This is great!'",
    parser = StructureFixingParser(retries = 3)
)
```

---

### History Compression

```kotlin
import ai.koog.agents.core.history.WholeHistory
import ai.koog.agents.core.history.RetrieveFactsFromHistory

// Simple case: keep everything (short agents)
val agent = AIAgent(
    ...,
    historyStrategy = WholeHistory()
)

// Long-running: extract key facts to reduce token usage
val agent = AIAgent(
    ...,
    historyStrategy = RetrieveFactsFromHistory(
        promptExecutor = executor,
        llmModel = DeepSeekModels.DeepSeekChat
    )
)

// In custom strategy: compress after tool-heavy steps
val myStrategy = strategy<String, String>("with-compression") {
    val toolNode   = node { ... }
    val compressor = nodeLLMCompressHistory()
    val finalNode  = node { ... }

    edge(toolNode, compressor)
    edge(compressor, finalNode)
    start(toolNode)
    finish(finalNode)
}
```

Set `preserveMemory = true` in agent config when using memory-aware compression.

---

### Streaming

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.llm.StreamFrame

// Collect streaming frames
agent.runStreaming("Tell me a story").collect { frame ->
    when (frame) {
        is StreamFrame.TextDelta -> print(frame.text) // real-time output
        is StreamFrame.ToolCall  -> println("\n[Tool: ${frame.name}]")
        is StreamFrame.Done      -> println("\n[Complete]")
    }
}

// Text-only streaming (ignore tool frames)
agent.runStreaming("Hello").filterTextOnly().collect { text ->
    print(text)
}
```

Hook into streaming events via `onLLMStreamingFrameReceived` in the agent event handler.

---

### Testing

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("ai.koog:agents-test:$koogVersion")
}

// Test setup
import ai.koog.agents.testing.getMockExecutor

val toolRegistry = ToolRegistry { tools(MyTools()) }

val mockExecutor = getMockExecutor(toolRegistry) {
    mockLLMAnswer("Paris") onRequestContains "capital of France"
    mockLLMAnswer("42") onRequestContains "meaning of life"
    mockLLMToolCall("search", mapOf("query" to "Kotlin")) onRequestContains "search for Kotlin"
}

val agent = AIAgent(
    promptExecutor = mockExecutor,
    llmModel = DeepSeekModels.DeepSeekChat,
    toolRegistry = toolRegistry,
    maxIterations = 10
) { withTesting() }

val result = agent.run("What is the capital of France?")
assertEquals("Paris", result)
```

---

### MCP Integration

```kotlin
import ai.koog.agents.mcp.McpToolRegistryProvider

// SSE transport (remote MCP server)
val mcpRegistry = McpToolRegistryProvider.fromTransport(
    transport = SseClientTransport(url = "http://localhost:3000/sse")
)

// stdio transport (local MCP process)
val mcpRegistry = McpToolRegistryProvider.fromTransport(
    transport = StdioClientTransport(command = listOf("npx", "my-mcp-server"))
)

// Merge with local tools
val combined = localRegistry + mcpRegistry

val agent = AIAgent(
    ...,
    toolRegistry = combined
)
```

---

### Multi-Agent (Agents as Tools)

```kotlin
// Wrap a specialized agent as a tool
@LLMDescription("Tools for delegating to specialized sub-agents")
class AgentTools(private val codeAgent: AIAgent) : ToolSet {

    @Tool
    @LLMDescription("Delegate a coding task to the code specialist agent")
    fun delegateCode(
        @LLMDescription("The coding task description") task: String
    ): String = runBlocking { codeAgent.run(task) }
}

// Orchestrator uses the wrapped agent as a tool
val orchestrator = AIAgent(
    ...,
    toolRegistry = ToolRegistry { tools(AgentTools(codeAgent)) }
)
```

For full A2A protocol (agent discovery, async delegation) use the `koog-a2a` artifact.

---

## Anti-Patterns / Common Mistakes

| Anti-Pattern | Why it's Wrong | Fix |
|---|---|---|
| Hardcoding API key in source | Security risk, can't rotate | `System.getenv("DEEPSEEK_API_KEY")` |
| Missing `@LLMDescription` on params | LLM doesn't know what to pass | Add `@LLMDescription` to every param |
| No `maxIterations` set | Agent can loop forever | Always set `maxIterations = N` |
| Using class-based tools on JVM | More verbose than needed | Use annotation-based `@Tool` on JVM |
| Raw JSON parsing of LLM output | Fragile, breaks on format variation | Use `requestLLMStructured` + `StructureFixingParser` |
| Parsing LLM text to extract data | String parsing is brittle | Define `@Serializable` response type |
| No history compression in long agents | Token limit exceeded, cost explosion | Add `nodeLLMCompressHistory` or `RetrieveFactsFromHistory` |
| Single giant system prompt | Hard to maintain, poor modularity | Split concerns: base prompt + dynamic context |
| Ignoring tool execution errors | Silent failures, bad UX | Return error strings; let LLM retry or escalate |

---

## Module / Dependency Guide

| Need | Artifact | Notes |
|---|---|---|
| Core agents + tools | `ai.koog:koog-agents` | Always required |
| Ktor server integration | `ai.koog:koog-ktor` | For web endpoints |
| Spring Boot integration | `ai.koog:koog-spring-boot-starter` | Auto-configuration |
| A2A multi-agent protocol | `ai.koog:koog-a2a` | Agent-to-agent comms |
| Mock testing utilities | `ai.koog:agents-test` | `testImplementation` only |
| MCP tool servers | `ai.koog:agent-mcp` | MCP client/server |

All versions come from `gradle/libs.versions.toml` — check `koog` version alias.

---

## Strategy Visualization (JVM only)

```kotlin
// Print strategy graph as Mermaid diagram for debugging
val diagram = myStrategy.asMermaidDiagram()
println(diagram)
// Paste into https://mermaid.live to visualize
```

---

## Quick Checklist Before Implementing

- [ ] Picked the right strategy for the use case (see decision tree above)
- [ ] All `@Tool` methods and params have `@LLMDescription`
- [ ] `maxIterations` is set
- [ ] Structured output uses `StructureFixingParser(retries = 3)`
- [ ] Long-running agents have history compression
- [ ] API key from env var, not hardcoded
- [ ] Tests use `getMockExecutor` from `agents-test`
- [ ] Tool return type is `String`
