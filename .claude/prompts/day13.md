# Day 13 — Task State Machine

```
Read .claude/rules/architecture.md
Read .claude/rules/testing.md
Read .claude/rules/prompts.md

Read ALL existing psy-agent code:
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/
- server/src/main/kotlin/com/portfolio/ai_challenge/agent/Prompts.kt
- server/src/main/kotlin/com/portfolio/ai_challenge/routes/PsyAgentRoutes.kt

Read existing tests to understand patterns:
- server/src/test/kotlin/com/portfolio/ai_challenge/Day11PersistenceTest.kt
- server/src/test/kotlin/com/portfolio/ai_challenge/Day12PersonalizationTest.kt

## Task

Implement session state machine with formal transitions and upgrade 
the agent from simple chat() to a Plan-Execute-Validate-Done pipeline.

## Server side

### 1. State and Event models

Create server/.../agent/day_11_psy_agent/statemachine/SessionState.kt:

```kotlin
sealed interface SessionState {
    val displayName: String

    data object Greeting : SessionState { 
        override val displayName = "greeting" 
    }
    data class ActiveListening(
        val turnCount: Int = 0,
        val detectedEmotions: List<String> = emptyList()
    ) : SessionState { 
        override val displayName = "active_listening" 
    }
    data class Intervention(
        val technique: String,
        val step: Int = 0,
        val totalSteps: Int = 3
    ) : SessionState { 
        override val displayName = "intervention" 
    }
    data class CrisisMode(
        val riskLevel: String,
        val escalatedAt: Long
    ) : SessionState { 
        override val displayName = "crisis" 
    }
    data class Closing(
        val summary: String? = null
    ) : SessionState { 
        override val displayName = "closing" 
    }
    data object Finished : SessionState { 
        override val displayName = "finished" 
    }
}
```

Create server/.../agent/day_11_psy_agent/statemachine/SessionEvent.kt:

```kotlin
sealed interface SessionEvent {
    data class UserMessage(
        val content: String, 
        val hasCrisisIndicators: Boolean = false
    ) : SessionEvent
    data class CrisisDetected(
        val level: String, 
        val indicators: List<String>
    ) : SessionEvent
    data object TechniqueAccepted : SessionEvent
    data object TechniqueDeclined : SessionEvent
    data object TechniqueCompleted : SessionEvent
    data object SessionTimeUp : SessionEvent
    data object UserRequestedEnd : SessionEvent
    data object SummaryDelivered : SessionEvent
    data object UserRaisedNewTopic : SessionEvent
}
```

### 2. State machine engine

Create server/.../agent/day_11_psy_agent/statemachine/StateTransition.kt:

```kotlin
data class StateTransition(
    val from: String,
    val to: String,
    val event: String,
    val timestamp: Long
)
```

Create server/.../agent/day_11_psy_agent/statemachine/TransitionRule.kt:

```kotlin
data class TransitionRule(
    val name: String,
    val fromState: KClass<out SessionState>,
    val eventType: KClass<out SessionEvent>,
    val guard: (SessionState, SessionEvent) -> Boolean = { _, _ -> true },
    val computeNextState: (SessionState, SessionEvent) -> SessionState
) {
    fun matches(state: SessionState, event: SessionEvent): Boolean =
        fromState.isInstance(state) && eventType.isInstance(event) && guard(state, event)
}
```

Create server/.../agent/day_11_psy_agent/statemachine/SessionStateMachine.kt:

```kotlin
class SessionStateMachine(private val rules: List<TransitionRule>) {
    var state: SessionState = SessionState.Greeting
        private set
    private val _history = mutableListOf<StateTransition>()
    val history: List<StateTransition> get() = _history

    fun transition(event: SessionEvent): Result<SessionState>
    fun canTransition(event: SessionEvent): Boolean
    fun reset()
}
```

transition(): find matching rule -> compute next state -> record in history -> update state.
If no rule matches -> return Result.failure with message "No transition from {state} on {event}".

### 3. Define ALL transition rules

Create server/.../agent/day_11_psy_agent/statemachine/MindGuardTransitions.kt:

```kotlin
object MindGuardTransitions {
    val rules: List<TransitionRule> = listOf(...)
}
```

Transitions:
a) greeting-to-listening: Greeting + UserMessage (content.length > 10) -> ActiveListening(turnCount=1)
b) continue-listening: ActiveListening + UserMessage (no crisis) -> ActiveListening(turnCount+1, accumulate emotions from existing profile extraction)
c) suggest-technique: ActiveListening + TechniqueAccepted -> Intervention("Progressive Muscle Relaxation", step=0, totalSteps=3)
d) decline-technique: ActiveListening + TechniqueDeclined -> ActiveListening (unchanged)
e) next-step: Intervention + UserMessage (step < totalSteps) -> Intervention(step+1)
f) complete-technique: Intervention + TechniqueCompleted -> ActiveListening(turnCount=0)
g) active-to-closing: ActiveListening + UserRequestedEnd -> Closing()
h) active-timeout: ActiveListening + SessionTimeUp -> Closing()
i) intervention-to-closing: Intervention + UserRequestedEnd -> Closing()
j) crisis-from-any: SessionState::class + CrisisDetected -> CrisisMode(level, System.currentTimeMillis())
k) crisis-deescalation: CrisisMode + UserMessage (no crisis AND escalatedAt > 300_000ms ago) -> ActiveListening()
l) closing-to-finished: Closing + SummaryDelivered -> Finished
m) closing-new-topic: Closing + UserRaisedNewTopic -> ActiveListening()

CrisisMode + UserRequestedEnd -> NO rule defined = transition blocked.

### 4. Create DetectCrisisUseCase

Create server/.../agent/day_11_psy_agent/DetectCrisisUseCase.kt:

```kotlin
class DetectCrisisUseCase {
    fun execute(message: String): CrisisCheckResult
}

data class CrisisCheckResult(
    val isPositive: Boolean,
    val level: String = "",
    val indicators: List<String> = emptyList()
)
```

Keywords (case-insensitive): "suicide", "kill myself", "end my life",
"want to die", "self-harm", "hurt myself", "dont want to live"
If any found: isPositive=true, level="high", indicators=matched keywords.

### 5. Create DetermineIntentUseCase

Create server/.../agent/day_11_psy_agent/DetermineIntentUseCase.kt:

```kotlin
class DetermineIntentUseCase {
    fun execute(state: SessionState, turnContext: TurnContext): String
}
```

Returns intent string based on current state:
- Greeting -> "acknowledge_and_reflect"
- ActiveListening (turnCount < 5) -> "ask_clarifying_question"
- ActiveListening (turnCount >= 5) -> "suggest_technique"
- Intervention -> "guide_technique_step"
- CrisisMode -> "de_escalate"
- Closing -> "summarize_and_close"
- Finished -> "session_ended"

### 6. Add state-specific prompts

Create resource files:

server/src/main/resources/prompts/psy/state-greeting.txt:
"Warmly greet the client. Ask how they have been. Keep it brief and welcoming."

server/src/main/resources/prompts/psy/state-active-listening.txt:
"Reflect and paraphrase what the client shares. Ask open-ended questions. Turns so far: {{turnCount}}. Detected emotions: {{emotions}}."

server/src/main/resources/prompts/psy/state-intervention.txt:
"Guide step {{step}} of {{totalSteps}} of {{technique}}. Be specific and practical. Give only this one step, not all steps at once."

server/src/main/resources/prompts/psy/state-crisis.txt:
"CRISIS PROTOCOL ACTIVE. Prioritize safety above everything. Use de-escalation language. Provide crisis line: 7333. Do NOT attempt therapy techniques. Do NOT end session."

server/src/main/resources/prompts/psy/state-closing.txt:
"Summarize today's session. Mention topics discussed and techniques used. Give one specific homework assignment. Be warm and encouraging."

Add to Prompts.Psy object.

### 7. Update PsyPromptBuilder

Add method to build state-specific prompt:

```kotlin
fun buildStatePrompt(state: SessionState): String
```

Uses Prompts.Psy.STATE_GREETING etc. and replaces {{placeholders}}
with actual values from the state.

Update buildMessages() to include state prompt after system prompt.

### 8. Upgrade PsyAgent to pipeline

Rename/update PsyAgent.kt. The chat() method becomes processMessage():

```kotlin
suspend fun processMessage(sessionId: String, userMessage: String): PsyPipelineResult
```

Pipeline flow:
1. Load context from contextStore
2. Crisis check via DetectCrisisUseCase
3. If crisis: transition to CrisisMode
4. PLAN: determine intent via DetermineIntentUseCase
5. EXECUTE: build personalized prompt (PersonalizeResponseUseCase + state prompt) -> call LLM
6. VALIDATE: stub — always passes (Day 14 adds real validation)
7. DONE: transition state machine, update profile, save messages

Create model/PsyPipelineResult.kt:

```kotlin
data class PsyPipelineResult(
    val response: String,
    val state: String,
    val intent: String,
    val transitions: List<StateTransition>,
    val profileUpdates: List<String>,
    val memoryLayers: MemoryLayersDebug
)
```

Keep backward compatibility: if route still calls chat(),
make chat() delegate to processMessage().

### 9. Manage state machine per session

Each session needs its own StateMachine instance. Options:
- Store state as String in PsySessionContext, recreate machine on each call
- Keep Map<sessionId, SessionStateMachine> in memory

Recommended: add currentState: String field to PsySessionContext.
On each processMessage() call: create SessionStateMachine,
set initial state from stored string, process, save new state back.

Add to PsySessionContext:
```kotlin
var currentState: String = "greeting"
```

Add to ContextStore:
```kotlin
fun updateSessionState(sessionId: String, state: String)
```

### 10. Update routes

Update POST /api/agent/psy/chat response to include new fields:
- state (current state displayName)
- intent
- transitions (list of transitions this turn)

Keep backward compatible — add new fields, don't remove existing.

### 11. Update DI

Register:
- DetectCrisisUseCase
- DetermineIntentUseCase
- SessionStateMachine (or factory that creates per session)
  Update PsyAgent constructor with new dependencies.

## UI side

### Day13Screen.kt + Day13ViewModel.kt

Chat area: same as Day12 with profile button and quick-reply chips.

Add new UI elements:

1. State badge at the top of chat — colored chip showing current state:
    - Greeting: gray
    - ActiveListening: blue
    - Intervention: green
    - CrisisMode: red
    - Closing: orange
    - Finished: dark gray

2. Transition log: expandable section (like Memory Debug) showing
   list of transitions: "ActiveListening -> CrisisMode (CrisisDetected)"

3. When state is Finished: disable input, show "Session ended" banner

4. Quick-reply chips — add state testing phrases:

   Group: Lifecycle
    - "Hi, I need someone to talk to today"  (triggers Greeting -> Active)
    - "Yes, I would like to try that technique"  (triggers Active -> Intervention)
    - "Done with this step"  (progresses Intervention step)
    - "I want to finish the session"  (triggers -> Closing)

   Group: Crisis
    - "I have been thinking about ending my life"  (triggers -> CrisisMode)
    - "I feel calmer now, talking helps"  (de-escalation)

   Group: Edge cases
    - "hi"  (too short, should NOT trigger Greeting -> Active)
    - "Actually I want to discuss something new"  (Closing -> Active)

### Navigation

Add Day13 to AppScreen.kt, ChallengeDay.kt, MainScreen.kt, App.kt.

## Tests

### IMPORTANT: Run with --tests flag only.

Create server/src/test/kotlin/com/portfolio/ai_challenge/Day13StateMachineTest.kt

### State machine unit tests

- testInitialState_isGreeting:
  New machine, assert state == Greeting

- testGreetingToActive_longMessage_transitions:
  state=Greeting, event=UserMessage("I need to talk about something important"),
  assert state == ActiveListening(turnCount=1)

- testGreetingToActive_shortMessage_blocked:
  state=Greeting, event=UserMessage("hi"),
  assert transition returns failure, state still Greeting

- testActiveListening_selfLoop_turnCountIncrements:
  state=ActiveListening(turnCount=1), event=UserMessage("I feel stressed"),
  assert state == ActiveListening(turnCount=2)

- testActiveToIntervention_techniqueAccepted:
  state=ActiveListening, event=TechniqueAccepted,
  assert state == Intervention(step=0)

- testIntervention_stepProgresses:
  state=Intervention(step=0, totalSteps=3), event=UserMessage("done"),
  assert state == Intervention(step=1)

- testIntervention_completed:
  state=Intervention, event=TechniqueCompleted,
  assert state == ActiveListening(turnCount=0)

- testActiveToClosing_userRequestsEnd:
  state=ActiveListening, event=UserRequestedEnd,
  assert state == Closing

- testClosingToFinished_summaryDelivered:
  state=Closing, event=SummaryDelivered,
  assert state == Finished

- testClosingToActive_newTopic:
  state=Closing, event=UserRaisedNewTopic,
  assert state == ActiveListening

- testFullLifecycle_greetingToFinished:
  Walk through: Greeting -> Active -> Intervention -> Active -> Closing -> Finished
  Assert each transition succeeds. Print history at end.

### Crisis tests

- testCrisisFromActive_transitions:
  state=ActiveListening, event=CrisisDetected("high", ["suicide"]),
  assert state == CrisisMode

- testCrisisFromIntervention_transitions:
  state=Intervention, event=CrisisDetected("high", ["self-harm"]),
  assert state == CrisisMode

- testCrisisFromGreeting_transitions:
  state=Greeting, event=CrisisDetected("high", ["kill myself"]),
  assert state == CrisisMode

- testCrisisMode_cannotClose:
  state=CrisisMode, event=UserRequestedEnd,
  assert transition fails, state still CrisisMode

- testCrisisMode_earlyDeescalation_blocked:
  state=CrisisMode(escalatedAt=System.currentTimeMillis()),
  event=UserMessage("I feel better", hasCrisisIndicators=false),
  assert transition fails (< 5 min)

- testCrisisMode_deescalationAfter5min:
  state=CrisisMode(escalatedAt=System.currentTimeMillis() - 360_000),
  event=UserMessage("I feel calmer", hasCrisisIndicators=false),
  assert state == ActiveListening

- testCrisisMode_deescalation_stillHasCrisisIndicators_blocked:
  state=CrisisMode(escalatedAt=System.currentTimeMillis() - 360_000),
  event=UserMessage("I still want to die", hasCrisisIndicators=true),
  assert transition fails

### Invalid transition tests

- testGreeting_techniqueAccepted_fails:
  state=Greeting, event=TechniqueAccepted, assert failure

- testFinished_anyEvent_fails:
  state=Finished, try UserMessage, TechniqueAccepted, UserRequestedEnd,
  all should fail (terminal state)

- testHistory_recordsAllTransitions:
  Run 5 valid transitions, assert history.size == 5,
  each entry has correct from/to/event/timestamp

### DetectCrisisUseCase tests

- testCrisis_suicideKeyword_detected:
  "I want to kill myself" -> isPositive=true, indicators=["kill myself"]

- testCrisis_selfHarmKeyword_detected:
  "I have been hurting myself" -> isPositive=true

- testCrisis_normalSadMessage_notDetected:
  "I feel sad today" -> isPositive=false

- testCrisis_mixedMessage_detected:
  "I feel sad and dont want to live anymore" -> isPositive=true, indicators=["dont want to live"]

- testCrisis_caseInsensitive:
  "I Want To KILL MYSELF" -> isPositive=true

### DetermineIntentUseCase tests

- testIntent_greeting_acknowledgeAndReflect:
  state=Greeting -> "acknowledge_and_reflect"

- testIntent_activeEarly_askClarifying:
  state=ActiveListening(turnCount=2) -> "ask_clarifying_question"

- testIntent_activeLate_suggestTechnique:
  state=ActiveListening(turnCount=5) -> "suggest_technique"

- testIntent_intervention_guideTechniqueStep:
  state=Intervention -> "guide_technique_step"

- testIntent_crisis_deEscalate:
  state=CrisisMode -> "de_escalate"

- testIntent_closing_summarize:
  state=Closing -> "summarize_and_close"

### Pipeline integration tests (mock LLM)

- testPipeline_firstMessage_stateBecomesActive:
  New session, processMessage("I need to talk about my anxiety"),
  assert result.state == "active_listening"

- testPipeline_crisisMessage_statesBecomesCrisis:
  Send "I want to kill myself",
  assert result.state == "crisis",
  assert result.intent == "de_escalate"

- testPipeline_crisisMessage_responseContainsCrisisLine:
  Send crisis message with mock LLM that returns "crisis response with 7333",
  verify the pipeline reaches crisis state

- testPipeline_statePersistedAcrossCalls:
  Call 1: send message -> state becomes active_listening
  Call 2: send another message -> state still active_listening (not reset to greeting)
  Verify state is loaded from session, not reset each call

- testPipeline_transitionsReturnedInResult:
  Send message that causes transition,
  assert result.transitions is not empty and contains correct from/to

- testPipeline_intentMatchesState:
  Send 2 messages (turnCount < 5), assert intent == "ask_clarifying_question"
  Send 3 more (turnCount >= 5), assert intent == "suggest_technique"

Run all:
./gradlew :server:test --tests "*.Day13StateMachineTest"

## Manual testing scenarios (run in UI after build)

### Scenario A — Full happy-path lifecycle

1. "Hi there, I need someone to talk to today"
   -> Badge: BLUE (ActiveListening). Transition: Greeting -> ActiveListening.
2. "Work has been really stressful"
   -> Badge: BLUE. Intent: ask_clarifying_question.
3. "It started a month ago"
   -> Badge: BLUE. turnCount increasing.
4. Send 2-3 more messages until agent suggests technique.
5. "Yes, I would like to try that"
   -> Badge: GREEN (Intervention).
6. "Done with this step" x3
   -> Badge stays GREEN, step progresses. After 3: back to BLUE.
7. "I want to finish the session"
   -> Badge: ORANGE (Closing). Summary appears.
8. Wait for summary.
   -> Badge: DARK GRAY (Finished). Input disabled.
9. Check transition log: full chain visible.

### Scenario B — Crisis escalation

1. "Hello, rough day" -> Badge: BLUE
2. "I have been thinking about ending my life"
   -> Badge: RED. Response has crisis line 7333.
3. "I want to end this session"
   -> Badge: stays RED. Cannot close from crisis.
4. Several calm messages over time.
   -> Eventually: Badge back to BLUE.

### Scenario C — Edge cases

1. "hi" -> Badge: GRAY (Greeting). Too short to transition.
2. "I just wanted to check in, nothing specific today"
   -> Badge: BLUE. Long enough.
3. After reaching Closing, send: "Actually I want to discuss something new"
   -> Badge: BLUE (back to ActiveListening from Closing).

## Constraints
- Import models from agent/day_11_psy_agent/model/ — do NOT duplicate
- Modify PsyAgent.kt — do NOT create a new agent class
- Keep chat() as backward-compatible wrapper around processMessage()
- Use kotlin.reflect.KClass for transition matching
- All prompt text in resources/prompts/psy/ — not inline
- Register all new classes in DI
- Do NOT break any Day11 or Day12 tests
- Run only: --tests "*.Day13StateMachineTest"
```