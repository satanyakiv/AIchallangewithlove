package com.portfolio.ai_challenge.experiment.models

import com.portfolio.ai_challenge.models.TokenUsage
import kotlinx.serialization.Serializable

@Serializable
data class StepResult(
    val stepId: String,
    val stepType: String,
    val failureMode: String? = null,
    val userMessage: String,
    val assistantResponse: String? = null,
    val usage: TokenUsage? = null,
    val httpStatus: Int,
    val errorMessage: String? = null,
    val conversationLength: Int,
    val timestamp: Long,
)
