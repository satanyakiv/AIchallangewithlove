package com.portfolio.ai_challenge.agent.psy_agent.model

import kotlinx.serialization.Serializable

// A summary of a completed session, stored in Layer 3 (user profile)
@Serializable
data class PsySessionSummary(
    val sessionId: String,
    val summaryText: String,
    val emotionsDetected: List<String> = emptyList(),
    val topicsDiscussed: List<String> = emptyList(),
    val techniquesUsed: List<String> = emptyList(),
    val homework: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
)
