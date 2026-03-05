package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

// A summary of a completed session, stored in Layer 3 (user profile)
data class PsySessionSummary(
    val sessionId: String,
    val summaryText: String,
    val emotionsDetected: List<String> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
)
