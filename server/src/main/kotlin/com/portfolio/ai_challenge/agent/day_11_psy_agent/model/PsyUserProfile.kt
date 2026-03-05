package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

// Layer 3 — persistent user profile (in-memory for Day 11)
data class PsyUserProfile(
    val userId: String,
    val preferredName: String? = null,
    val primaryConcerns: List<String> = emptyList(),
    val preferredTechniques: List<String> = emptyList(),
    val sessionHistory: List<PsySessionSummary> = emptyList(),
)
