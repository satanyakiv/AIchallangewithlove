# SessionStateMachine — State Transitions

## Key Files

| File | Role |
|------|------|
| `statemachine/SessionState.kt` | Sealed interface — all possible states |
| `statemachine/SessionEvent.kt` | Sealed interface — all possible events |
| `statemachine/TransitionRule.kt` | Data class — a single transition rule |
| `statemachine/MindGuardTransitions.kt` | List of all 13 rules (`mindGuardTransitions`) |
| `statemachine/SessionStateMachine.kt` | State machine — executes transitions |

---

## States

```
Greeting → ActiveListening → Intervention → Closing → Finished
                ↑___________________|
                         ↕
                      CrisisMode
```

| State | Data |
|-------|------|
| `Greeting` | — |
| `ActiveListening` | `turnCount`, `detectedEmotions` |
| `Intervention` | `technique`, `step`, `totalSteps` |
| `CrisisMode` | `riskLevel`, `escalatedAt` |
| `Closing` | `summary?` |
| `Finished` | — (terminal) |

---

## Events (SessionEvent)

| Event | When |
|-------|------|
| `UserMessage(content, hasCrisisIndicators)` | User sent a message |
| `CrisisDetected(riskLevel, indicators)` | Agent detected a crisis |
| `CrisisResolved` | Crisis resolved (cooldown elapsed) |
| `TechniqueProposed(technique, totalSteps)` | Agent proposed a technique |
| `TechniqueAccepted` | User agreed to try the technique |
| `StepCompleted` | One step of the technique was completed |
| `TechniqueCompleted` | All steps completed |
| `SessionEndRequested` | User or agent wants to end the session |

---

## How State Transitions Work

### Full Chain

```
Agent calls stateMachine.transition(event)
        ↓
SessionStateMachine iterates rules in order
        ↓
For each TransitionRule, rule.matches(state, event) is checked:
  1. anyState || fromState.isInstance(state)  ← correct source state?
  2. event.isInstance(evt)                    ← correct event type?
  3. guard(state, evt)                        ← guard condition satisfied?
        ↓
First matching rule → rule.nextState(state, event)
        ↓
state = nextState                  ← state is updated
_history.add(StateTransition(...)) ← recorded in history log
        ↓
Result.success(nextState)          ← new state returned

If no rule matches:
Result.failure(IllegalStateException)
```

### Code (SessionStateMachine.kt)

```kotlin
fun transition(event: SessionEvent): Result<SessionState> {
    val rule = rules.firstOrNull { it.matches(state, event) }
        ?: return Result.failure(IllegalStateException("No transition from ${state.displayName} on ${event::class.simpleName}"))
    val nextState = rule.nextState(state, event)
    _history.add(StateTransition(from = state.displayName, to = nextState.displayName, event = event::class.simpleName ?: "Unknown"))
    state = nextState
    return Result.success(nextState)
}
```

---

## All 13 Rules (MindGuardTransitions)

| # | From | Event | Guard | To |
|---|------|-------|-------|----|
| a | `Greeting` | `UserMessage` | `length > 10 && !hasCrisis` | `ActiveListening(turnCount=1)` |
| b | `ActiveListening` | `UserMessage` | `!hasCrisis` | `ActiveListening(turnCount+1)` |
| c | `ActiveListening` | `TechniqueProposed` | — | `Intervention(technique, step=0)` |
| d | `Intervention` | `TechniqueAccepted` | — | `Intervention(step=1)` |
| e | `Intervention` | `StepCompleted` | `step < totalSteps` | `Intervention(step+1)` |
| f | `Intervention` | `TechniqueCompleted` | — | `ActiveListening()` |
| g | `ActiveListening` | `SessionEndRequested` | — | `Closing()` |
| h | `Intervention` | `SessionEndRequested` | — | `Closing()` |
| i | `Closing` | `SessionEndRequested` | — | `Finished` |
| j | **any** | `CrisisDetected` | `state !is Finished` | `CrisisMode(riskLevel)` |
| k | `CrisisMode` | `CrisisResolved` | `cooldown > 5 min` | `ActiveListening()` |
| l | `CrisisMode` | `SessionEndRequested` | — | **blocked** (no rule exists) |
| m | `Greeting` | `UserMessage` | `length <= 10` | **stays in Greeting** (no rule exists) |

> Rules l and m are implicit: the absence of a matching rule means the transition is blocked.

---

## Transition Examples

### Normal message from Greeting

```
State:  Greeting
Event:  UserMessage("I've been feeling really low today", hasCrisisIndicators = false)

Rule (a):
  fromState = Greeting ✓
  event = UserMessage ✓
  guard: "I've been feeling really low today".length (34) > 10 ✓, !hasCrisisIndicators ✓

Result: ActiveListening(turnCount = 1)
```

### Crisis from any state (rule j)

```kotlin
TransitionRule(
    anyState = true,              // ignores fromState
    event = CrisisDetected::class,
    guard = { state, _ -> state !is SessionState.Finished }
)
```

```
State:  ActiveListening(turnCount = 5)
Event:  CrisisDetected(riskLevel = "high", indicators = ["harm", "suicide"])

anyState = true → fromState is ignored ✓
event = CrisisDetected ✓
guard: ActiveListening !is Finished ✓

Result: CrisisMode(riskLevel = "high", escalatedAt = <timestamp>)
```

### Blocked exit from CrisisMode

```
State:  CrisisMode(riskLevel = "high")
Event:  SessionEndRequested

No rule matches (CrisisMode + SessionEndRequested)
→ Result.failure("No transition from crisis on SessionEndRequested")
```

---

## canTransition vs transition

```kotlin
// Check without transitioning (for UI / agent logic)
if (stateMachine.canTransition(SessionEvent.SessionEndRequested)) {
    stateMachine.transition(SessionEvent.SessionEndRequested)
}

// transition returns Result.failure on its own if the transition is not allowed
val result = stateMachine.transition(event)
result.onFailure { /* handle blocked transition */ }
```

`canTransition` is a **public query method** for external callers.
It is intentionally not used inside `transition` — to avoid searching through rules twice.

---

## State Serialization (persistence)

State is stored as a string via `ContextStore.updateSessionState`:

```
Greeting             → "greeting"
ActiveListening(3)   → "active_listening:3"
Intervention(...)    → "intervention:breathing:2:3"
CrisisMode(...)      → "crisis:high:1709123456789"
Closing              → "closing"
Finished             → "finished"
```

Restore: `SessionState.fromStorageString(s)` → `stateMachine.restoreState(state)`
