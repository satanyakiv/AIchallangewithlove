package com.portfolio.ai_challenge.agent.freud_agent.model

data class FreudProfileUpdate(
    val patientName: String?,
    val newDreamSymbols: List<String>,
    val newDefenseMechanisms: List<String>,
    val newChildhoodThemes: List<String>,
    val detectedFixation: String?,
    val newRelationshipPatterns: List<String>,
    val detectedLanguage: String = "en",
)
