# Architecture Rules

> Read before writing any server feature.

## Layered Architecture

Every server feature MUST have these layers in separate files:

```
Routes (HTTP)  →  Agent (orchestration)  →  Components (logic)  →  Store (data)
```

**Routes** (`routes/XxxRoutes.kt`) — HTTP only: request/response DTOs, parsing, status codes. No business logic.

**Agent** (`agent/xxx/XxxAgent.kt`) — Orchestration only. Reads like a recipe:

```kotlin
suspend fun chat(sessionId: String, message: String): ChatResult {
  val context = contextStore.assembleContext(sessionId)
  val prompt = promptBuilder.build(context, message)
  val response = llmClient.complete(prompt)
  contextStore.save(sessionId, message, response)
  return responseMapper.toResult(context, response)
}
```

Never put in Agent: HTTP calls, prompt strings, JSON serialization, DTOs.

**Components** — separate file per responsibility:

| File | Does |
|------|------|
| `XxxPromptBuilder.kt` | Builds prompts from loaded templates + runtime data |
| `XxxResponseMapper.kt` | Internal models → API response DTOs |
| `LlmClient.kt` | DeepSeek HTTP wrapper (shared across agents) |
| `XxxValidator.kt` | Validates responses (invariants, safety) |

**Store** (`memory/`, `database/`) — Data access only. Interface first, implementation second.

## Dependency Injection

Never create dependencies inside a class. Inject via constructor, register in Koin.

```kotlin
// BAD
class PsyAgent { private val store = InMemoryContextStore() }

// GOOD
class PsyAgent(private val contextStore: ContextStore, private val llmClient: LlmClient)
```

## Sealed Types Over Strings

```kotlin
// BAD
val state: String = "active_listening"

// GOOD
sealed interface SessionState {
    data object Greeting : SessionState
    data class ActiveListening(val turnCount: Int) : SessionState
}
```

### Zero Hardcoded Strings for Types

If an enum, sealed class, or sealed interface exists for a concept — NEVER pass its values as strings. Use the type directly. If the type doesn't exist yet — create it first.

```kotlin
// BAD — string comparison against enum values
private fun phaseOrder(name: String): Int = when (name) {
    "assessment" -> 0
    "plan_proposed" -> 1
    "executing" -> 2
    else -> -1
}
fun execute(phase: TaskPhase, event: SessionEvent): PhaseCheck {
    return requireAtLeast(phase, "plan_proposed")  // string!
}

// GOOD — use the type, add ordinal/comparison to the enum itself
enum class TaskPhase(val order: Int) {
    ASSESSMENT(0),
    PLAN_PROPOSED(1),
    EXECUTING(2),
    VALIDATING(3),
    COMPLETED(4);
}
fun execute(phase: TaskPhase, event: SessionEvent): PhaseCheck {
    return requireAtLeast(phase, TaskPhase.PLAN_PROPOSED)  // type-safe
}
private fun requireAtLeast(current: TaskPhase, required: TaskPhase): PhaseCheck {
    return if (current.order >= required.order) PhaseCheck.Allowed
    else PhaseCheck.Blocked("Need ${required.name}", required)
}
```

**Rules:**
- Never pass `enum.name` or `sealed.displayName` as String parameter. Pass the type.
- Never `when` on strings that match enum/sealed values. `when` on the type.
- If you need ordering — add `order: Int` property to the enum.
- If you need display text — add `displayName: String` property to the enum.
- If a matching enum/sealed type doesn't exist — create it before using strings.

## Size Limits

- Files: < 150 lines. If more — split.
- Functions: < 20 lines. If more — extract helpers.
- Agent class: < 80 lines. If more — extract to components.

## Mapper Pattern (AtoBMapper)

> See full rules: `.claude/rules/mapping.md`

Use a dedicated `{A}To{B}Mapper` class with a `map(source: A): B` method for any type conversion.
Never inline conversions in Agent, UseCase, or Route.
Return typed enums/sealed interfaces — never raw `String`.

## Use Cases

When an Agent method grows beyond simple orchestration — extract a UseCase class.

**Signal to extract**: agent does logic beyond "call A, pass result to B" — conditions, loops, data transformation, extraction.

```kotlin
// BAD — agent has extraction logic inside chat()
class PsyAgent(...) {
  suspend fun chat(sessionId: String, msg: String): PsyChatResult {
    val nameRegex = Regex("(?i)(?:my name is|call me|i am)\\s+(\\w+)")
    val match = nameRegex.find(msg)
    if (match != null) {
      contextStore.updateUserProfile(userId) { it.copy(preferredName = match.groupValues[1]) }
    }
  }
}

// GOOD — extracted to UseCase
class UpdateProfileUseCase(private val contextStore: ContextStore) {
  fun execute(userId: String, message: String): ProfileUpdate { ... }
}

class PsyAgent(
  private val updateProfile: UpdateProfileUseCase,
) {
  suspend fun chat(sessionId: String, msg: String): PsyChatResult {
    val profileUpdate = updateProfile.execute(session.userId, msg)
  }
}
```

**Naming**: `{Verb}{Noun}UseCase.kt` — e.g. `UpdateProfileUseCase`, `ExtractEmotionsUseCase`, `DetectCrisisUseCase`.

Each UseCase:
- One public method: `execute(...)`
- Own file in the feature package
- Independently testable

## File Organization

```
server/src/main/kotlin/com/portfolio/ai_challenge/
  agent/
    psy/
      PsyAgent.kt                # orchestration only
      PsyPromptBuilder.kt        # prompt composition
      PsyResponseMapper.kt       # internal → DTO mapping
      prompts/
        PsyPrompts.kt            # loads .txt prompt files
      model/
        PsySessionContext.kt     # one class per file
        PsyUserProfile.kt
      memory/
        ContextStore.kt          # interface
        InMemoryContextStore.kt  # implementation
      statemachine/
        SessionState.kt
        SessionStateMachine.kt
      invariants/
        Invariant.kt
        InvariantChecker.kt
        impl/
          NoDiagnosisInvariant.kt
  models/
    LlmClient.kt                # shared DeepSeek wrapper
    DeepSeekModels.kt
  routes/
    PsyAgentRoutes.kt           # HTTP layer + DTOs

server/src/main/resources/
  prompts/psy/
    system.txt
    state-greeting.txt
    state-active-listening.txt
    state-intervention.txt
    state-crisis.txt
    state-closing.txt
```