package com.portfolio.ai_challenge.agent.psy_agent.model

/**
 * Typed intent derived from [com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState].
 *
 * Exposed in [PsyChatResult] so the UI state badge and transition log can display
 * what the agent is doing this turn without leaking raw state internals.
 *
 * [apiName] is the stable string used for HTTP serialization — never change existing values.
 */
sealed interface SessionIntent {
    val apiName: String

    data object Welcome : SessionIntent {
        override val apiName = "welcome"
    }

    data object ActiveListening : SessionIntent {
        override val apiName = "active_listening"
    }

    /** Step is dynamic, so [InterventionStep] is a data class rather than object. */
    data class InterventionStep(val step: Int) : SessionIntent {
        override val apiName = "intervention_step_$step"
    }

    data object CrisisSupport : SessionIntent {
        override val apiName = "crisis_support"
    }

    data object SessionClosing : SessionIntent {
        override val apiName = "session_closing"
    }

    data object SessionFinished : SessionIntent {
        override val apiName = "session_finished"
    }
}
