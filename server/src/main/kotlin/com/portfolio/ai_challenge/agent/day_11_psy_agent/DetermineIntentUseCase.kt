package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionState

/**
 * Maps the current [SessionState] to a human-readable intent string.
 *
 * The intent is returned inside [PsyChatResult] and exposed to the UI so the
 * state badge and transition log can display what the agent is doing this turn.
 *
 * | State | Intent |
 * |-------|--------|
 * | Greeting | `welcome` |
 * | ActiveListening | `active_listening` |
 * | Intervention(step=N) | `intervention_step_N` |
 * | CrisisMode | `crisis_support` |
 * | Closing | `session_closing` |
 * | Finished | `session_finished` |
 */
class DetermineIntentUseCase {

    fun execute(state: SessionState, turnContext: TurnContext): String = when (state) {
        is SessionState.Greeting -> "welcome"
        is SessionState.ActiveListening -> "active_listening"
        is SessionState.Intervention -> "intervention_step_${state.step}"
        is SessionState.CrisisMode -> "crisis_support"
        is SessionState.Closing -> "session_closing"
        is SessionState.Finished -> "session_finished"
    }
}
