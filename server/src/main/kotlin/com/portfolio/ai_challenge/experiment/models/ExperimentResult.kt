package com.portfolio.ai_challenge.experiment.models

import com.portfolio.ai_challenge.models.TokenUsage
import kotlinx.serialization.Serializable

@Serializable
data class ConversationEntry(
    val role: String,       // "user" or "assistant"
    val content: String,
    val stepId: String,
    val stepType: String,
    val usage: TokenUsage? = null,  // null for user messages
    val timestamp: Long,
)

@Serializable
data class ExperimentResult(
    val caseName: String,
    val steps: List<StepResult>,
    val conversationLog: List<ConversationEntry>,
    val totalMessages: Int,
    val peakTokens: Int,
    val totalSteps: Int,
    val failedSteps: Int,
    val startedAt: Long,
    val finishedAt: Long,
)

@Serializable
data class FullExperimentResult(
    val cases: List<ExperimentResult>,
    val generatedAt: Long,
)
