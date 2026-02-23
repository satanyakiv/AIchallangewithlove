package com.portfolio.ai_challange_with_love.models

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
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String,
)

@Serializable
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice>,
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
