package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.model.FreudSessionIntent
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState

class FreudStateToIntentMapper {

    fun map(state: FreudSessionState): FreudSessionIntent = when (state) {
        is FreudSessionState.Begruessung -> FreudSessionIntent.Welcome
        is FreudSessionState.FreeAssociation -> FreudSessionIntent.Probing
        is FreudSessionState.Interpretation -> FreudSessionIntent.Interpreting
        is FreudSessionState.DreamAnalysis -> FreudSessionIntent.AnalyzingDream
        is FreudSessionState.Transference -> FreudSessionIntent.AddressingTransference
        is FreudSessionState.Abschluss -> FreudSessionIntent.Farewell
    }
}
