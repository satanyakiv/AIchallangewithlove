# Mapper Pattern (AtoBMapper)

When converting one type to another — use a dedicated Mapper class. Never inline type conversion in Agent, UseCase, or Route.

```kotlin
// BAD — conversion inline in agent
val intent: String = when (state) {
    is SessionState.Greeting -> "welcome"
    ...
}

// GOOD — dedicated mapper
class SessionStateToIntentMapper {
    fun map(state: SessionState): SessionIntent = when (state) {
        is SessionState.Greeting -> SessionIntent.Welcome
        ...
    }
}
```

## Rules

- Naming: `{SourceType}To{TargetType}Mapper.kt` — e.g. `SessionStateToIntentMapper`, `PsyChatResultToResponseMapper`
- One public method: `map(source: A): B`
- Own file in the feature package
- Independently testable — no external dependencies needed

## Mapper vs UseCase

| | Mapper | UseCase |
|-|--------|---------|
| Purpose | Pure structural conversion | Logic, orchestration |
| Side effects | None | Allowed (data access, external calls) |
| Dependencies | None | ContextStore, LlmClient, etc. |
| Method name | `map(source: A): B` | `execute(...): Result` |

## Return Types: Enum/Sealed over String

Mapper output must be a typed enum or sealed interface — never a raw `String`.

```kotlin
// BAD
fun map(state: SessionState): String = "welcome"

// GOOD
fun map(state: SessionState): SessionIntent = SessionIntent.Welcome
```

Use `apiName: String` on the sealed type for HTTP serialization:

```kotlin
sealed interface SessionIntent {
    val apiName: String
    data object Welcome : SessionIntent { override val apiName = "welcome" }
}

// In ResponseMapper (HTTP layer only):
intent = result.intent.apiName
```
