package com.portfolio.ai_challenge.agent.freud_agent.model

import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudStateTransition
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext

data class FreudChatResult(
    val response: String,
    val state: String,
    val session: PsySessionContext,
    val profile: FreudUserProfile,
    val turnContext: FreudTurnContext,
    val profileUpdate: FreudProfileUpdate,
    val intent: FreudSessionIntent = FreudSessionIntent.Welcome,
    val transitions: List<FreudStateTransition> = emptyList(),
)
