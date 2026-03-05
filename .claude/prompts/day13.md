Read the architecture diagrams in documentation/day-11-to-15/:
- psy-agent-state-diagram.md — PRIMARY REFERENCE for states and transitions
- psy-agent-class-diagram.md
- psy-agent-sequence-diagram.md

Read documentation/project-structure.md.
Read ALL existing psy-agent code in server/.../agent/psy/ and composeApp/.../ui/screen/.

## Task

Implement the session state machine with formal transitions and a Plan-Execute-Validate-Done pipeline.

## Server side

### State models: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/statemachine/

SessionState.kt:
```kotlin
sealed interface SessionState {
    val displayName: String
    data object Greeting : SessionState { override val displayName = "greeting" }
    data class ActiveListening(
        val turnCount: Int = 0,
        val detectedEmotions: List<String> = emptyList()
    ) : SessionState { override val displayName = "active_listening" }
    data class Intervention(
        val technique: String,
        val step: Int = 0,
        val totalSteps: Int = 3
    ) : SessionState { override val displayName = "intervention" }
    data class CrisisMode(
        val riskLevel: String,
        val escalatedAt: Long
    ) : SessionState { override val displayName = "crisis" }
    data class Closing(
        val summary: String? = null
    ) : SessionState { override val displayName = "closing" }
    data object Finished : SessionState { override val displayName = "finished" }
}
```

SessionEvent.kt:
```kotlin
sealed interface SessionEvent {
    data class UserMessage(val content: String, val hasCrisisIndicators: Boolean = false) : SessionEvent
    data class CrisisDetected(val level: String, val indicators: List<String>) : SessionEvent
    data object TechniqueAccepted : SessionEvent
    data object TechniqueDeclined : SessionEvent
    data object TechniqueCompleted : SessionEvent
    data object SessionTimeUp : SessionEvent
    data object UserRequestedEnd : SessionEvent
    data object SummaryDelivered : SessionEvent
    data object UserRaisedNewTopic : SessionEvent
}
```

StateTransition.kt:
- from: String (state displayName)
- to: String (state displayName)
- event: String (event class name)
- timestamp: Long

### State machine: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/statemachine/

TransitionRule.kt:
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

SessionStateMachine.kt:
- var state: SessionState (starts as Greeting)
- val history: List<StateTransition>
- fun transition(event: SessionEvent): Result<SessionState>
- fun canTransition(event: SessionEvent): Boolean
- fun reset()

MindGuardTransitions.kt — object with val rules: List<TransitionRule>:

Define ALL transitions from state diagram:
a) greeting-to-listening: Greeting + UserMessage (content.length > 10) -> ActiveListening(turnCount=1)
b) continue-listening: ActiveListening + UserMessage (no crisis) -> ActiveListening(turnCount+1)
c) suggest-technique: ActiveListening + TechniqueAccepted -> Intervention("PMR", step=0)
d) decline-technique: ActiveListening + TechniqueDeclined -> ActiveListening (unchanged)
e) next-step: Intervention + UserMessage (step < totalSteps) -> Intervention(step+1)
f) complete-technique: Intervention + TechniqueCompleted -> ActiveListening(turnCount=0)
g) active-to-closing: ActiveListening + UserRequestedEnd -> Closing()
h) active-timeout: ActiveListening + SessionTimeUp -> Closing()
i) intervention-to-closing: Intervention + UserRequestedEnd -> Closing()
j) crisis-from-any: SessionState::class + CrisisDetected -> CrisisMode(level, now)
k) crisis-deescalation: CrisisMode + UserMessage (no crisis AND escalatedAt > 300000ms ago) -> ActiveListening()
l) closing-to-finished: Closing + SummaryDelivered -> Finished
m) closing-new-topic: Closing + UserRaisedNewTopic -> ActiveListening()

Guard: UserRequestedEnd from CrisisMode -> blocked (no rule defined = cannot transition).

### Crisis detector: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/statemachine/

CrisisDetector.kt:
- fun check(message: String): CrisisCheckResult
- CrisisCheckResult: isPositive: Boolean, level: String, indicators: List<String>
- Keywords (case-insensitive): "suicide", "kill myself", "end my life", "want to die", "self-harm", "hurt myself"
- If any found: isPositive=true, level="high", indicators=matched keywords

### Pipeline: server/src/main/kotlin/com/portfolio/ai_challenge/agent/psy/

Update PsyAgent.kt — replace simple chat() with pipeline:
```kotlin
suspend fun processMessage(sessionId: String, userMessage: String): PsyPipelineResult
```

PsyPipelineResult.kt (in agent/psy/model/):
- response: String
- state: String (displayName)
- intent: String
- phase: String ("plan" / "execute" / "validate" / "done")
- transitions: List<StateTransition>
- memoryLayers: MemoryLayersDebug

Pipeline flow:
1. Load context from ContextStore
2. Crisis check on user message
3. If crisis detected: transition to CrisisMode
4. PLAN: determine intent based on current state
    - Greeting -> "acknowledge_and_reflect"
    - ActiveListening (turnCount < 5) -> "ask_clarifying_question"
    - ActiveListening (turnCount >= 5) -> "suggest_technique"
    - Intervention -> "guide_technique_step"
    - CrisisMode -> "de_escalate"
    - Closing -> "summarize_and_close"
5. EXECUTE: build prompt (PersonalizedPromptBuilder + state-specific instructions + context) -> call DeepSeek
6. VALIDATE: stub, always passes (Day 14 adds real validation)
7. DONE: transition state machine, save to context store, extract profile updates

State-specific prompt additions:
- Greeting: "Warmly greet the client. Ask how they have been."
- ActiveListening: "Reflect and paraphrase. Ask open-ended questions. Turns so far: {turnCount}."
- Intervention: "Guide step {step}/{totalSteps} of {technique}. Be specific and practical."
- CrisisMode: "CRISIS PROTOCOL. Prioritize safety. Provide crisis line: 7333. Use de-escalation language."
- Closing: "Summarize the session. Mention techniques used. Give homework."

### Update route

POST /api/agent/psy/chat response now returns PsyPipelineResult fields.

## UI side

### Update PsyAgent screen (or create Day13Screen.kt)

Add to the chat screen:
- State indicator badge at the top showing current state (color-coded):
    - Greeting: gray
    - ActiveListening: blue
    - Intervention: green
    - CrisisMode: red
    - Closing: orange
    - Finished: dark gray
- Transition log: expandable section showing history of state transitions
- When state is Finished, disable input and show "Session ended" message

### Navigation

Add Day13 to AppScreen.kt, ChallengeDay.kt, MainScreen.kt, App.kt.

## Tests

### Server: server/src/test/kotlin/com/portfolio/ai_challenge/

Day13StateMachineTest.kt:
- testFullLifecycle: Greeting -> Active -> Intervention -> Active -> Closing -> Finished
- testCrisisFromActive: ActiveListening + CrisisDetected -> CrisisMode
- testCrisisFromIntervention: Intervention + CrisisDetected -> CrisisMode
- testCrisisGuard5min: CrisisMode + UserMessage immediately -> fail. Set escalatedAt 6 min ago -> success
- testCannotCloseFromCrisis: CrisisMode + UserRequestedEnd -> fail (no matching rule)
- testInvalidTransition: Greeting + TechniqueAccepted -> failure
- testHistory: 5 transitions -> history has 5 entries
- testCrisisDetector: "I want to kill myself" -> positive. "I feel sad today" -> negative

Day13PipelineTest.kt:
- testPipelineGreeting: new session, send message -> response + state = active_listening
- testPipelineCrisis: send crisis message -> state = crisis, response contains crisis line

## Constraints
- Import models from agent/psy/model/ — do NOT duplicate
- Modify PsyAgent.kt — do NOT create a new agent class
- Use kotlin.reflect.KClass for transition matching
- Add @Serializable to StateTransition for API response
- Follow existing route patterns