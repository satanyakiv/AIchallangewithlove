# Architecture Rules

> Referenced from CLAUDE.md. Read this before writing any server feature.

## Layered Architecture

Every server feature MUST have these layers in separate files:

```
Routes (HTTP)  →  Agent (orchestration)  →  Components (logic)  →  Store (data)
```

### Layer 1 — Routes (`routes/XxxRoutes.kt`)

HTTP only: request parsing, response serialization, status codes.
Request/response DTOs (`@Serializable` data classes) live here.
Never contains business logic, prompt strings, or direct API calls.

### Layer 2 — Agent (`agent/xxx/XxxAgent.kt`)

Orchestration only. Should read like a recipe:

```kotlin
// GOOD — pure orchestration, each line is one step
suspend fun chat(sessionId: String, message: String): ChatResult {
    val context = contextStore.assembleContext(sessionId)
    val prompt = promptBuilder.build(context, message)
    val response = llmClient.complete(prompt)
    val validated = validator.check(response)
    contextStore.save(sessionId, message, validated)
    return responseMapper.toResult(context, validated)
}
```

Never put these inside the Agent class:
- HTTP calls (use LlmClient)
- Prompt string literals (use PromptBuilder)
- JSON serialization (use ResponseMapper)
- Request/response DTOs (those live in Routes)

### Layer 3 — Components (separate file per responsibility)

| File | Responsibility |
|------|---------------|
| `XxxPromptBuilder.kt` | Builds system prompts, context, message lists |
| `XxxResponseMapper.kt` | Maps internal models → API response DTOs |
| `LlmClient.kt` | DeepSeek HTTP wrapper, shared across agents |
| `XxxValidator.kt` | Validates responses (invariants, safety) |

### Layer 4 — Store (`memory/`, `database/`)

Data access only. Always: interface first, implementation second.

```kotlin
interface ContextStore { ... }
class InMemoryContextStore : ContextStore { ... }
```

## Anti-Patterns

```kotlin
// BAD — agent does HTTP, serialization, prompt building, and storage
class PsyAgent(private val httpClient: HttpClient) {
    suspend fun chat(sessionId: String, msg: String): PsyChatResponse {
        val prompt = "You are MindGuard..."  // prompt literal in agent
        val httpResp = httpClient.post("https://api.deepseek.com/...") {
            setBody(json.encodeToString(...))  // serialization in agent
        }
        sessions[sessionId]!!.messages.add(msg)  // raw state mutation
        return PsyChatResponse(...)  // DTO construction in agent
    }
}

// GOOD — agent is 6 lines of orchestration
class PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val responseMapper: PsyResponseMapper,
) {
    suspend fun chat(sessionId: String, msg: String): ChatResult {
        val context = contextStore.assembleContext(sessionId)
        val prompt = promptBuilder.build(context, msg)
        val response = llmClient.complete(prompt)
        contextStore.save(sessionId, msg, response)
        return responseMapper.toResult(context, response)
    }
}
```

## Dependency Injection

Never create dependencies inside a class:

```kotlin
// BAD
class PsyAgent {
    private val store = InMemoryContextStore()
}

// GOOD — inject via constructor, register in Koin
class PsyAgent(
    private val contextStore: ContextStore,
    private val llmClient: LlmClient,
)
```

## Sealed Types Over Strings

```kotlin
// BAD
val state: String = "active_listening"
if (state == "crisis") { ... }

// GOOD
sealed interface SessionState {
    data object Greeting : SessionState
    data class ActiveListening(val turnCount: Int) : SessionState
    data class CrisisMode(val riskLevel: String) : SessionState
}
when (state) {
    is CrisisMode -> { ... }  // exhaustive, compiler-checked
}
```

## File Organization

```
server/src/main/kotlin/com/portfolio/ai_challenge/
  agent/
    psy/                          # feature package
      PsyAgent.kt                # orchestration (< 80 lines)
      PsyPromptBuilder.kt        # prompt construction
      PsyResponseMapper.kt       # internal → DTO mapping
      model/                      # one file per data class
        PsySessionContext.kt
        PsyUserProfile.kt
        ...
      memory/
        ContextStore.kt           # interface
        InMemoryContextStore.kt   # implementation
      statemachine/
        SessionState.kt
        SessionEvent.kt
        SessionStateMachine.kt
        ...
      invariants/
        Invariant.kt
        InvariantChecker.kt
        impl/
          NoDiagnosisInvariant.kt
          ...
  models/
    LlmClient.kt                 # shared DeepSeek wrapper
    DeepSeekModels.kt
  routes/
    PsyAgentRoutes.kt            # HTTP layer + DTOs
```