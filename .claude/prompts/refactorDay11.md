# Refactor: Day 11 Psy-Agent Server Architecture

Read `.claude/rules/architecture.md` first. Then read ALL files under `server/src/main/kotlin/com/portfolio/ai_challenge/agent/` related to the psy-agent.

## Problem

The current PsyAgent.kt mixes multiple responsibilities:
- API request/response DTOs (PsyStartRequest, PsyChatResponse, MemoryLayersDebug)
- DeepSeek HTTP calls (callDeepSeek method with HttpClient, bearerAuth, serialization)
- Prompt construction (SYSTEM_PROMPT constant, context prompt building)
- Response mapping (building MemoryLayersDebug from internal state)
- Orchestration (chat flow)

This violates the layered architecture. Refactor into separate files.

## Target structure

```
server/src/main/kotlin/com/portfolio/ai_challenge/
  agent/psy/
    PsyAgent.kt              — orchestration ONLY (~30-40 lines)
    PsyPromptBuilder.kt      — SYSTEM_PROMPT, buildMessages(), buildContextPrompt()
    PsyResponseMapper.kt     — toStartResponse(), toChatResponse(), buildMemoryDebug()
    model/                    — one file per data class (keep existing)
    memory/                   — ContextStore, InMemoryContextStore (keep existing)
  models/
    LlmClient.kt             — NEW: shared DeepSeek wrapper (extracted from PsyAgent)
  routes/
    PsyAgentRoutes.kt        — move DTOs here (PsyStartRequest, PsyChatRequest, PsyChatResponse, etc.)
```

## Step by step

### 1. Extract LlmClient (shared, reusable by future agents)

Create `server/src/main/kotlin/com/portfolio/ai_challenge/models/LlmClient.kt`:

```kotlin
class LlmClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val apiUrl: String = "https://api.deepseek.com/chat/completions",
    private val model: String = "deepseek-chat",
) {
    suspend fun complete(messages: List<DeepSeekMessage>, temperature: Double = 0.7): String
}
```

Move the callDeepSeek logic here. This class owns HttpClient, apiKey, JSON serialization.
Register as singleton in Koin.

### 2. Extract PsyPromptBuilder

Create `server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/PsyPromptBuilder.kt`:

```kotlin
class PsyPromptBuilder {
    fun buildSystemPrompt(): String              // returns the SYSTEM_PROMPT
    fun buildContextPrompt(context: PsyAgentContext): String  // uses ContextWindowManager internally
    fun buildMessages(context: PsyAgentContext, userMessage: String): List<DeepSeekMessage>
}
```

Move SYSTEM_PROMPT constant here. Move the message list construction from chat() here.
Inject ContextWindowManager into PsyPromptBuilder.

### 3. Extract PsyResponseMapper

Create `server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/PsyResponseMapper.kt`:

```kotlin
class PsyResponseMapper {
    fun toChatResponse(context: PsyAgentContext, response: String, turnContext: TurnContext): PsyChatResponse
    fun buildMemoryDebug(session: PsySessionContext, profile: PsyUserProfile, turn: TurnContext): MemoryLayersDebug
}
```

Move the MemoryLayersDebug construction here.

### 4. Move DTOs to Routes

Move these data classes from PsyAgent.kt to PsyAgentRoutes.kt (or the file where routes are defined):
- PsyStartRequest
- PsyStartResponse
- PsyChatRequest
- PsyChatResponse
- MemoryLayersDebug

These are HTTP-layer concerns. The agent should not know about them.
PsyAgent should return an internal result type. PsyResponseMapper converts it to PsyChatResponse.

Define an internal result:
```kotlin
// In agent/psy/model/
data class PsyChatResult(
    val response: String,
    val state: String,
    val session: PsySessionContext,
    val profile: PsyUserProfile,
    val turnContext: TurnContext,
)
```

Route calls agent.chat() -> gets PsyChatResult -> uses PsyResponseMapper to build PsyChatResponse.

### 5. Slim down PsyAgent

After extraction, PsyAgent.kt should look like:

```kotlin
class PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
) {
    fun startSession(userId: String): String {
        val sessionId = UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val turnContext = TurnContext(attemptCount = session.messages.count { it.role == "user" } + 1)
        contextStore.appendMessage(sessionId, ConversationEntry(role = "user", content = userMessage))
        val context = contextStore.assembleContext(sessionId, "active")
        val messages = promptBuilder.buildMessages(context, userMessage)
        val response = llmClient.complete(messages)
        contextStore.appendMessage(sessionId, ConversationEntry(role = "assistant", content = response))
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(context.userId)
        return PsyChatResult(response, "active", updatedSession, profile, turnContext)
    }
}
```

No HTTP. No prompt strings. No DTO construction. Pure orchestration.

### 6. Update Koin DI

Register new classes:
- LlmClient (singleton, shared)
- PsyPromptBuilder (factory or singleton)
- PsyResponseMapper (factory or singleton)
- Update PsyAgent constructor to use new deps

### 7. Update routes

Route now does:
```kotlin
val result = psyAgent.chat(request.sessionId, request.message)
val response = responseMapper.toChatResponse(result)
call.respond(response)
```

## Verification

After refactoring:
1. Run `./gradlew :server:test` — all existing tests must pass
2. Run `./gradlew :server:run` and test manually:
    - POST /api/agent/psy/start -> returns sessionId
    - POST /api/agent/psy/chat -> returns response with memoryLayers
    - Second message in same session -> context is preserved
3. Verify no file exceeds 150 lines
4. Verify PsyAgent.kt has ZERO imports from io.ktor.client (no HTTP)
5. Verify PsyAgent.kt has ZERO string literals containing prompt text

## Constraints

- Do NOT change any behavior or API contracts — same endpoints, same request/response format
- Do NOT rename packages unless necessary for the new structure
- Do NOT touch files outside of server/ module
- Keep all existing tests passing
- Register all new classes in Koin