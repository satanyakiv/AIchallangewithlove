package com.portfolio.ai_challenge.agent.psy_agent.model

import com.portfolio.ai_challenge.models.MessageRole

// A single message stored in Layer 2 (session memory)
data class ConversationEntry(
    val role: MessageRole,
    val content: String,
    val timestampMs: Long = System.currentTimeMillis(),
)
