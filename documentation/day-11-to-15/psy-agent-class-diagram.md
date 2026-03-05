classDiagram
direction TB

    %% ═══════════════════════════════════
    %% STATE MACHINE
    %% ═══════════════════════════════════

    class SessionState {
        <<sealed interface>>
    }
    class Greeting {
        <<data object>>
    }
    class ActiveListening {
        +turnCount: Int
        +detectedEmotions: List~Emotion~
    }
    class Intervention {
        +technique: Technique
        +step: Int
    }
    class CrisisMode {
        +riskLevel: RiskLevel
        +escalatedAt: Instant
    }
    class Closing {
        +summary: SessionSummary?
    }
    class Finished {
        <<data object>>
    }

    SessionState <|.. Greeting
    SessionState <|.. ActiveListening
    SessionState <|.. Intervention
    SessionState <|.. CrisisMode
    SessionState <|.. Closing
    SessionState <|.. Finished

    class SessionEvent {
        <<sealed interface>>
    }
    class UserMessage {
        +content: String
        +analysis: MessageAnalysis
    }
    class CrisisDetected {
        +level: RiskLevel
        +indicators: List~String~
    }

    SessionEvent <|.. UserMessage
    SessionEvent <|.. CrisisDetected
    SessionEvent <|.. TechniqueAccepted
    SessionEvent <|.. TechniqueDeclined
    SessionEvent <|.. SessionTimeUp
    SessionEvent <|.. UserRequestedEnd

    class SessionStateMachine {
        -state: SessionState
        -history: List~StateTransition~
        +transition(event, context): Result~SessionState~
    }

    class TransitionRule {
        +name: String
        +fromState: KClass
        +event: KClass
        +guard(): Boolean
        +computeNextState(): SessionState
    }

    SessionStateMachine --> "1" SessionState : current
    SessionStateMachine --> "*" TransitionRule : rules
    SessionStateMachine --> "1" InvariantChecker : validates transitions
    TransitionRule ..> SessionState : from/to
    TransitionRule ..> SessionEvent : triggered by

    %% ═══════════════════════════════════
    %% PIPELINE
    %% ═══════════════════════════════════

    class AgentPipeline {
        -maxRetries: Int = 3
        +processMessage(msg): AgentResponse
        -plan(msg, ctx): ResponsePlan
        -execute(plan, ctx, violation?): AgentResponse
        -validate(response, ctx): InvariantViolation?
        -done(response, plan, ctx): AgentResponse
        -fallbackResponse(ctx): AgentResponse
    }

    class ResponsePlan {
        +intent: ResponseIntent
        +emotionalContext: EmotionalContext
        +stateTransition: SessionEvent?
        +techniques: List~Technique~
        +constraints: List~String~
    }

    class AgentResponse {
        +content: String
        +plan: ResponsePlan
        +metadata: ResponseMetadata
    }

    AgentPipeline --> SessionStateMachine : reads/updates state
    AgentPipeline --> InvariantChecker : validates responses
    AgentPipeline --> ContextStore : loads/saves context
    AgentPipeline ..> ResponsePlan : creates
    AgentPipeline ..> AgentResponse : produces

    %% ═══════════════════════════════════
    %% INVARIANTS
    %% ═══════════════════════════════════

    class Invariant {
        <<interface>>
        +name: String
        +rationale: String
        +check(response, context): InvariantResult
    }

    class InvariantResult {
        <<sealed interface>>
    }
    class Passed {
        <<data object>>
    }
    class Violated {
        +message: String
        +severity: Severity
    }

    InvariantResult <|.. Passed
    InvariantResult <|.. Violated

    class Severity {
        <<enum>>
        HARD_BLOCK
        SOFT_FIX
        WARNING
    }

    Violated --> Severity

    class InvariantChecker {
        -invariants: List~Invariant~
        +check(response, context): InvariantViolation?
    }

    class NoDiagnosisInvariant {
        +check(): InvariantResult
    }
    class NoMedicationInvariant {
        +check(): InvariantResult
    }
    class EmpathyToneInvariant {
        +check(): InvariantResult
    }
    class ConfidentialityInvariant {
        +check(): InvariantResult
    }

    Invariant <|.. NoDiagnosisInvariant
    Invariant <|.. NoMedicationInvariant
    Invariant <|.. EmpathyToneInvariant
    Invariant <|.. ConfidentialityInvariant

    InvariantChecker --> "*" Invariant : checks all
    InvariantChecker ..> InvariantResult : returns

    %% ═══════════════════════════════════
    %% CONTEXT
    %% ═══════════════════════════════════

    class ContextStore {
        <<interface>>
        +loadSession(id): SessionContext?
        +saveSession(session)
        +loadUserProfile(id): UserProfile?
        +assembleContext(sessionId): AgentContext
    }

    class AgentContext {
        +sessionId: String
        +currentState: SessionState
        +conversationHistory: List~Message~
        +userProfile: UserProfile
        +availableTechniques: List~Technique~
    }

    ContextStore ..> AgentContext : assembles