package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

data class PsyChatResult(
    val response: String,
    val state: String,
    val session: PsySessionContext,
    val profile: PsyUserProfile,
    val turnContext: TurnContext,
)
