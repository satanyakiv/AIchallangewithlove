package com.portfolio.ai_challange_with_love.data

import com.portfolio.ai_challange_with_love.SERVER_PORT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModelCompareRequest(val prompt: String)

@Serializable
data class ModelMetrics(
    val modelId: String = "",
    val modelLabel: String = "",
    val responseTimeMs: Long = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val estimatedCost: Double = 0.0,
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
data class ModelCompareAnalyzeRequest(val results: List<ModelResultForAnalysis>)

@Serializable
data class ModelCompareAnalysis(val comparison: String = "")

sealed class ModelStreamEvent {
    data class ModelStart(val model: String, val label: String) : ModelStreamEvent()
    data class ModelResult(val index: Int, val label: String, val content: String) : ModelStreamEvent()
    data class Metrics(val index: Int, val data: ModelMetrics) : ModelStreamEvent()
    data object Done : ModelStreamEvent()
    data class Error(val message: String) : ModelStreamEvent()
}

@Serializable
private data class SseModelStart(val type: String, val model: String = "", val label: String = "")

@Serializable
private data class SseModelResult(val type: String, val index: Int = 0, val label: String = "", val content: String = "")

@Serializable
private data class SseMetrics(val type: String, val index: Int = 0, val data: ModelMetrics? = null)

@Serializable
private data class SseGeneric(val type: String, val message: String? = null)

private val sseJson = Json { ignoreUnknownKeys = true }

class ModelApi(private val client: HttpClient) {
    private val baseUrl = "http://${getServerHost()}:$SERVER_PORT"

    suspend fun streamModelComparison(
        prompt: String,
        onEvent: (ModelStreamEvent) -> Unit,
    ) {
        client.preparePost("$baseUrl/api/models/stream") {
            contentType(ContentType.Application.Json)
            setBody(ModelCompareRequest(prompt))
            timeout { requestTimeoutMillis = 300_000 }
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data.isEmpty()) continue

                try {
                    val generic = sseJson.decodeFromString<SseGeneric>(data)
                    when (generic.type) {
                        "model_start" -> {
                            val evt = sseJson.decodeFromString<SseModelStart>(data)
                            onEvent(ModelStreamEvent.ModelStart(evt.model, evt.label))
                        }
                        "model_result" -> {
                            val evt = sseJson.decodeFromString<SseModelResult>(data)
                            onEvent(ModelStreamEvent.ModelResult(evt.index, evt.label, evt.content))
                        }
                        "metrics" -> {
                            val evt = sseJson.decodeFromString<SseMetrics>(data)
                            evt.data?.let { onEvent(ModelStreamEvent.Metrics(evt.index, it)) }
                        }
                        "done" -> onEvent(ModelStreamEvent.Done)
                        "error" -> onEvent(ModelStreamEvent.Error(generic.message ?: "Unknown error"))
                    }
                } catch (_: Exception) {
                    continue
                }
            }
        }
    }

    suspend fun analyzeModelResults(results: List<ModelResultForAnalysis>): ModelCompareAnalysis {
        return client.post("$baseUrl/api/models/analyze") {
            contentType(ContentType.Application.Json)
            setBody(ModelCompareAnalyzeRequest(results))
        }.body()
    }
}