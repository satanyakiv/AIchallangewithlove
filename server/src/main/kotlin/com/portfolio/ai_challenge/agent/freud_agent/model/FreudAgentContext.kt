package com.portfolio.ai_challenge.agent.freud_agent.model

import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry

data class FreudAgentContext(
    val sessionId: String,
    val userId: String,
    val currentState: String,
    val currentMessages: List<ConversationEntry>,
    val userProfile: FreudUserProfile,
)
