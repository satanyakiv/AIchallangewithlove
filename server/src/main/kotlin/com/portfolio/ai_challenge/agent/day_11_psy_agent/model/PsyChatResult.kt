package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.StateTransition

data class PsyChatResult(
    val response: String,
    val state: String,
    val session: PsySessionContext,
    val profile: PsyUserProfile,
    val turnContext: TurnContext,
    val profileUpdates: List<String> = emptyList(),
    val intent: String = "",
    val transitions: List<StateTransition> = emptyList(),
)
