package com.portfolio.ai_challenge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Our API contract (client <-> server) ---

@Serializable
data class TemperatureRequest(
    val prompt: String,
    val temperature: Double,
)

@Serializable
data class TemperatureResponse(
    val temperature: Double,
    val content: String,
    val error: String? = null,
)

@Serializable
data class AnalyzeRequest(
    val results: List<TemperatureResult>,
)

@Serializable
data class TemperatureResult(
    val temperature: Double,
    val content: String,
)

@Serializable
data class AnalyzeResponse(
    val comparison: String,
    val recommendations: List<Recommendation>,
)

@Serializable
data class Recommendation(
    val temperature: Double,
    val bestFor: String,
)

// --- DeepSeek API models ---

@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<DeepSeekMessage>,
    val temperature: Double,
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String,
)

@Serializable
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice>,
    val usage: TokenUsage? = null,
)

@Serializable
data class DeepSeekChoice(
    val message: DeepSeekMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

// --- Streaming models ---

@Serializable
data class DeepSeekStreamChunk(
    val choices: List<DeepSeekStreamChoice>,
)

@Serializable
data class DeepSeekStreamChoice(
    val delta: DeepSeekDelta,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class DeepSeekDelta(
    val content: String? = null,
)

// --- Model comparison models (Day 5) ---

@Serializable
data class ModelCompareRequest(
    val prompt: String,
)

@Serializable
data class ModelMetrics(
    val modelId: String,
    val modelLabel: String,
    val responseTimeMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val estimatedCost: Double,
)

@Serializable
data class ModelCompareAnalyzeRequest(
    val results: List<ModelResultForAnalysis>,
)

@Serializable
data class ModelResultForAnalysis(
    val modelLabel: String,
    val content: String,
    val responseTimeMs: Long,
    val totalTokens: Int,
    val estimatedCost: Double,
)

@Serializable
data class ModelCompareAnalysis(
    val comparison: String,
)

// DeepSeek usage in non-streaming response
@Serializable
data class DeepSeekUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0,
)

@Serializable
data class DeepSeekFullResponse(
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage? = null,
)
