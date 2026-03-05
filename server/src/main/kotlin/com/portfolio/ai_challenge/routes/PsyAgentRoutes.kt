package com.portfolio.ai_challenge.routes

import kotlinx.serialization.Serializable

@Serializable
data class PsyStartRequest(val userId: String)

@Serializable
data class PsyStartResponse(val sessionId: String)

@Serializable
data class PsyChatRequest(val sessionId: String, val message: String)

@Serializable
data class PsyPreferencesRequest(
    val userId: String,
    val language: String = "en",
    val formality: String = "INFORMAL",
    val responseLength: String = "MEDIUM",
    val avoidTopics: List<String> = emptyList(),
)
