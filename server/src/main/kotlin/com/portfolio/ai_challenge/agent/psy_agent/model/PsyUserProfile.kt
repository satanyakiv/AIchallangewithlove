package com.portfolio.ai_challenge.agent.psy_agent.model

import kotlinx.serialization.Serializable

// Layer 3 — persistent user profile (in-memory for Day 11+)
@Serializable
data class PsyUserProfile(
    val userId: String,
    val preferredName: String? = null,
    val primaryConcerns: List<String> = emptyList(),
    val knownTriggers: List<String> = emptyList(),
    val preferredTechniques: List<String> = emptyList(),
    val sessionHistory: List<PsySessionSummary> = emptyList(),
    val preferences: CommunicationPreferences = CommunicationPreferences(),
)

@Serializable
data class CommunicationPreferences(
    val language: String = "en",
    val formality: Formality = Formality.INFORMAL,
    val responseLength: ResponseLength = ResponseLength.MEDIUM,
    val avoidTopics: List<String> = emptyList(),
)

@Serializable
enum class Formality { FORMAL, INFORMAL, MIXED }

@Serializable
enum class ResponseLength { SHORT, MEDIUM, DETAILED }
