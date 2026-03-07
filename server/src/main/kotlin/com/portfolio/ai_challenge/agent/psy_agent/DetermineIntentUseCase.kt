package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.model.SessionIntent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState

/**
 * Maps [SessionState] → [SessionIntent].
 *
 * The intent is returned inside [com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult]
 * and exposed to the UI so the state badge and transition log can display what the agent is doing
 * this turn.
 *
 * | State | Intent |
 * |-------|--------|
 * | Greeting | [SessionIntent.Welcome] |
 * | ActiveListening | [SessionIntent.ActiveListening] |
 * | Intervention(step=N) | [SessionIntent.InterventionStep] |
 * | CrisisMode | [SessionIntent.CrisisSupport] |
 * | Closing | [SessionIntent.SessionClosing] |
 * | Finished | [SessionIntent.SessionFinished] |
 */
class SessionStateToIntentMapper {

    fun map(state: SessionState): SessionIntent = when (state) {
        is SessionState.Greeting -> SessionIntent.Welcome
        is SessionState.ActiveListening -> SessionIntent.ActiveListening
        is SessionState.Intervention -> SessionIntent.InterventionStep(state.step)
        is SessionState.CrisisMode -> SessionIntent.CrisisSupport
        is SessionState.Closing -> SessionIntent.SessionClosing
        is SessionState.Finished -> SessionIntent.SessionFinished
    }
}
