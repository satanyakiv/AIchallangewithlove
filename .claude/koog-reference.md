# Koog Reference — Kotlin AI Agent Framework by JetBrains

Version: **0.6.3** | Docs: https://docs.koog.ai/ | API: https://api.koog.ai/
GitHub: https://github.com/JetBrains/koog

## Dependency

```kotlin
// build.gradle.kts
repositories { mavenCentral() }
dependencies {
    implementation("ai.koog:koog-agents:0.6.3")
}
```

Additional modules: `koog-ktor`, `koog-spring-boot-starter`, `koog-a2a`.
Nightly repo: `https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public`

## DeepSeek Setup (project default)

```kotlin
val apiKey = System.getenv("DEEPSEEK_API_KEY")
    ?: error("DEEPSEEK_API_KEY is not set")

val client = DeepSeekLLMClient(apiKey)
val agent = AIAgent(
    promptExecutor = SingleLLMPromptExecutor(client),
    llmModel = DeepSeekModels.DeepSeekChat,
    systemPrompt = "You are a helpful assistant.",
    temperature = 0.7,
    toolRegistry = ToolRegistry { /* tools here */ },
    maxIterations = 30
)
val result = agent.run("Hello!")
```

## Other LLM Providers

| Provider | Executor | Model Example |
|----------|----------|--------------|
| OpenAI | `simpleOpenAIExecutor(key)` | `OpenAIModels.Chat.GPT4o` |
| Anthropic | `simpleAnthropicExecutor(key)` | `AnthropicModels.Opus_4_1` |
| Google | `simpleGoogleAIExecutor(key)` | `GoogleModels.Gemini2_5Pro` |
| Ollama | `simpleOllamaAIExecutor()` | `OllamaModels.Meta.LLAMA_3_2` |
| Bedrock | `simpleBedrockExecutor(access, secret)` | `BedrockModels.AnthropicClaude4_5Sonnet` |

Env vars: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `GOOGLE_API_KEY`, `DEEPSEEK_API_KEY`, `OPENROUTER_API_KEY`

## AIAgent Constructor

```kotlin
AIAgent(
    promptExecutor: PromptExecutor,           // required
    llmModel: LLMModel,                       // required
    systemPrompt: String = "",                 // system message
    temperature: Double = 1.0,                 // 0.0-2.0
    toolRegistry: ToolRegistry = ToolRegistry {},
    maxIterations: Int = 50,                   // max tool loops
    strategy: AgentStrategy? = null            // custom strategy graph
)
```

## Prompts

```kotlin
val prompt = prompt("unique_id") {
    system("You are a helpful assistant.")
    user("Question")
    assistant("Answer")  // for few-shot
}

// With LLM params
val prompt = prompt("id", params = LLMParams(
    temperature = 0.7,
    numberOfChoices = 1,
    toolChoice = LLMParams.ToolChoice.Auto
)) {
    system("You are a creative assistant.")
    user("Write a poem.")
}

// Extend existing prompt
val extended = prompt(basePrompt) { user("Follow-up question") }
```

### Text builders inside messages:
```kotlin
user {
    +"Line of text"
    br()
    text("inline text")
    markdown { h2("Title"); bulleted { item { +"item1" } } }
    xml { tag("root") { text("content") } }
    padding("  ") { +"indented" }
}
```

### Tool messages (for few-shot with tools):
```kotlin
tool {
    call(id = "calc_1", tool = "calculator", content = """{"a": 5, "b": 3}""")
    result(id = "calc_1", tool = "calculator", content = "8")
}
```

## Tools — Annotation-Based (preferred)

```kotlin
@LLMDescription("Weather information tools")
class WeatherTools : ToolSet {
    @Tool
    @LLMDescription("Get current weather for a location")
    fun getWeather(
        @LLMDescription("City and country") location: String
    ): String {
        return "Sunny, 22°C in $location"
    }
}

// Register
val agent = AIAgent(
    toolRegistry = ToolRegistry { tools(WeatherTools()) },
    // ...
)
```

Key rules:
- Class implements `ToolSet`
- Each tool function annotated with `@Tool`
- Use `@LLMDescription` on class, function, and parameters
- Optional params get default values
- Return `String`

## Tools — Class-Based

```kotlin
@Serializable
data class BookArgs(val title: String, val author: String)

class BookTool : SimpleTool<BookArgs>(
    argsSerializer = BookArgs.serializer(),
    name = "book",
    description = "Parse book information"
) {
    override suspend fun execute(args: BookArgs): String {
        return "Found: ${args.title} by ${args.author}"
    }
}

// Register
ToolRegistry { tool(BookTool()) }
```

## Tool Registry

```kotlin
val reg1 = ToolRegistry { tool(SayToUser); tools(WeatherTools()) }
val reg2 = ToolRegistry { tool(BookTool()) }
val merged = reg1 + reg2  // merge registries
```

## Functional Agents (custom control flow)

```kotlin
val agent = AIAgent<String, String>(
    systemPrompt = "Math assistant",
    promptExecutor = simpleOpenAIExecutor(key),
    llmModel = OpenAIModels.Chat.GPT4o,
    strategy = functionalStrategy { input ->
        val response = requestLLM(input)
        response.asAssistantMessage().content
    }
)
```

### With tool execution loop:
```kotlin
strategy = functionalStrategy { input ->
    var responses = requestLLMMultiple(input)
    while (responses.containsToolCalls()) {
        val calls = extractToolCalls(responses)
        val results = executeMultipleTools(calls)
        responses = sendMultipleToolResults(results)
    }
    responses.single().asAssistantMessage().content
}
```

### Sequential LLM calls (chain-of-thought):
```kotlin
strategy = functionalStrategy { input ->
    val draft = requestLLM("Draft: $input").asAssistantMessage().content
    val improved = requestLLM("Improve and clarify.").asAssistantMessage().content
    requestLLM("Format result.").asAssistantMessage().content
}
```

## Structured Output

```kotlin
@Serializable
@SerialName("WeatherForecast")
@LLMDescription("Weather forecast")
data class WeatherForecast(
    @property:LLMDescription("Temperature in Celsius") val temperature: Int,
    @property:LLMDescription("Conditions") val conditions: String,
    @property:LLMDescription("Precipitation %") val precipitation: Int
)

// Via prompt executor
val result = promptExecutor.executeStructured<WeatherForecast>(
    prompt = prompt("forecast") {
        system("You are a weather assistant.")
        user("Forecast for Amsterdam?")
    },
    model = OpenAIModels.Chat.GPT4oMini,
    fixingParser = StructureFixingParser(model = OpenAIModels.Chat.GPT4o, retries = 3)
)

// Inside agent LLM context
val result = llm.writeSession {
    requestLLMStructured<WeatherForecast>(
        fixingParser = StructureFixingParser(model = OpenAIModels.Chat.GPT4o, retries = 3)
    )
}
```

Supports: `@Serializable` data classes, sealed classes (polymorphism), enums, nested classes, `List<T>`, `Map<K,V>`.

## Strategy Graphs (advanced)

```kotlin
val strategy = strategy<String, String>("my-strategy") {
    val callLLM by nodeLLMRequest()
    val executeTool by nodeExecuteTool()
    val sendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo callLLM)
    edge(callLLM forwardTo nodeFinish onAssistantMessage { true })
    edge(callLLM forwardTo executeTool onToolCall { true })
    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo callLLM)
}
```

Shorthand: `nodeStart then nodeA then nodeB then nodeFinish`

## History Compression

Built-in strategies:
```kotlin
// Compress whole history into summary
val compress by nodeLLMCompressHistory<T>(strategy = HistoryCompressionStrategy.WholeHistory)

// Keep last N messages, compress rest
val compress by nodeLLMCompressHistory<T>(strategy = HistoryCompressionStrategy.FromLastNMessages(5))

// Chunk-based compression
val compress by nodeLLMCompressHistory<T>(strategy = HistoryCompressionStrategy.Chunked(10))

// Extract specific facts
val compress by nodeLLMCompressHistory<T>(strategy = RetrieveFactsFromHistory(
    Concept(keyword = "prefs", description = "User preferences", factType = FactType.MULTIPLE),
    Concept(keyword = "solved", description = "Issue resolved?", factType = FactType.SINGLE)
))
```

Preserve memory across compressions: `preserveMemory = true`

Manual: `llm.writeSession { replaceHistoryWithTLDR() }`

## Documentation Map

| Topic | URL |
|-------|-----|
| Getting started | https://docs.koog.ai/getting-started/ |
| Basic agents | https://docs.koog.ai/basic-agents/ |
| Functional agents | https://docs.koog.ai/functional-agents/ |
| Prompts | https://docs.koog.ai/prompts/prompt-creation/ |
| LLM clients | https://docs.koog.ai/prompts/llm-clients/ |
| Tools overview | https://docs.koog.ai/tools-overview/ |
| Annotation tools | https://docs.koog.ai/annotation-based-tools/ |
| Class tools | https://docs.koog.ai/class-based-tools/ |
| Structured output | https://docs.koog.ai/structured-output/ |
| Strategy graphs | https://docs.koog.ai/predefined-agent-strategies/ |
| Custom strategies | https://docs.koog.ai/custom-strategy-graphs/ |
| History compression | https://docs.koog.ai/history-compression/ |
| MCP integration | https://docs.koog.ai/model-context-protocol/ |
| A2A protocol | https://docs.koog.ai/a2a-protocol-overview/ |
| Streaming | https://docs.koog.ai/streaming-api/ |
| Testing | https://docs.koog.ai/testing/ |
| Ktor plugin | https://docs.koog.ai/ktor-plugin/ |
| Spring Boot | https://docs.koog.ai/spring-boot/ |
| Examples | https://docs.koog.ai/examples/ |
| API reference | https://api.koog.ai/ |

## Best Practices

1. **Use annotation-based tools** — simpler, less boilerplate than class-based
2. **Always add `@LLMDescription`** on tools, classes, and parameters — LLM needs clear descriptions
3. **Set `maxIterations`** to prevent infinite loops
4. **Use `ToolRegistry` merging** (`+`) to compose tool sets
5. **Use structured output** for typed responses instead of parsing strings
6. **Use history compression** for long conversations to save tokens
7. **Use `functionalStrategy`** when you need custom control flow (retries, chains)
8. **Use strategy graphs** for complex multi-step workflows with branching
9. **Use `StructureFixingParser`** with retries for robust structured output parsing
10. **Prefer `simpleXxxExecutor()`** helpers for quick setup; use `XxxLLMClient` directly for advanced config