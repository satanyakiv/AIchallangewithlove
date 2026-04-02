package com.portfolio.ai_challenge.agent.freud_agent.model

data class FreudTurnContext(
    val attemptCount: Int = 1,
    val detectedMarker: String? = null,
    val plan: String = "",
)
