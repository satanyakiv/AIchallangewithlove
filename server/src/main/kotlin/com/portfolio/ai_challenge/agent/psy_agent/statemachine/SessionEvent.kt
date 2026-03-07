package com.portfolio.ai_challenge.agent.psy_agent.statemachine

sealed interface SessionEvent {
    /** User sent a message (normal flow) */
    data class UserMessage(
        val content: String,
        val hasCrisisIndicators: Boolean = false,
    ) : SessionEvent

    /** Crisis detected in user message */
    data class CrisisDetected(val riskLevel: String, val indicators: List<String>) : SessionEvent

    /** Therapist/agent proposes a technique */
    data class TechniqueProposed(val technique: String, val totalSteps: Int = 3) : SessionEvent

    /** User accepts / agrees to try the technique */
    data object TechniqueAccepted : SessionEvent

    /** One step of a technique was completed */
    data object StepCompleted : SessionEvent

    /** All steps of the current technique are done */
    data object TechniqueCompleted : SessionEvent

    /** Crisis indicators are gone and cooldown passed */
    data object CrisisResolved : SessionEvent

    /** User or agent wants to end the session */
    data object SessionEndRequested : SessionEvent
}
