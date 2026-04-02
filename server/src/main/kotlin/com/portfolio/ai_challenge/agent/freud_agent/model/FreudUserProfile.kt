package com.portfolio.ai_challenge.agent.freud_agent.model

import kotlinx.serialization.Serializable

@Serializable
data class FreudUserProfile(
    val userId: String,
    val patientName: String? = null,
    val defenseMechanisms: List<String> = emptyList(),
    val childhoodThemes: List<String> = emptyList(),
    val dreamSymbols: List<String> = emptyList(),
    val fixationStage: String? = null,
    val relationshipPatterns: List<String> = emptyList(),
)
