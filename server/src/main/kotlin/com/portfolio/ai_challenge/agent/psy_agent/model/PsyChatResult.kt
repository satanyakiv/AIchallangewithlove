package com.portfolio.ai_challenge.agent.psy_agent.model

import com.portfolio.ai_challenge.agent.psy_agent.statemachine.StateTransition

data class PsyChatResult(
    val response: String,
    val state: String,
    val session: PsySessionContext,
    val profile: PsyUserProfile,
    val turnContext: TurnContext,
    val profileUpdates: List<String> = emptyList(),
    val intent: SessionIntent = SessionIntent.Welcome,
    val transitions: List<StateTransition> = emptyList(),
    val violations: List<String> = emptyList(),
)
