package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

// Layer 1 — ephemeral, lives only for the duration of a single request
data class TurnContext(
    val plan: String? = null,
    val violation: String? = null,
    val attemptCount: Int = 0,
    val detectedEmotion: String? = null,
)
