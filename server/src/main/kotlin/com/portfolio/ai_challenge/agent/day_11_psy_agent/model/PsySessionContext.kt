package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

// Layer 2 — in-memory session state for the current conversation
data class PsySessionContext(
    val sessionId: String,
    val userId: String,
    val state: String = "active", // "active" | "ended"
    val messages: List<ConversationEntry> = emptyList(),
    val detectedEmotions: List<String> = emptyList(),
    val suggestedTechniques: List<String> = emptyList(),
)
