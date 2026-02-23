package com.portfolio.ai_challange_with_love.routes

import com.portfolio.ai_challange_with_love.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private data class ModelConfig(val id: String, val label: String, val maxTokens: Int?)

private val MODEL_TIERS = listOf(
    ModelConfig("deepseek-chat", "Weak (chat, 100 tokens)", 100),
    ModelConfig("deepseek-chat", "Medium (chat, full)", null),
    ModelConfig("deepseek-reasoner", "Strong (reasoner)", null),
)

// Price per 1M tokens
private const val INPUT_PRICE = 0.28
private const val OUTPUT_PRICE = 0.42

private fun estimateCost(promptTokens: Int, completionTokens: Int): Double {
    return (promptTokens * INPUT_PRICE + completionTokens * OUTPUT_PRICE) / 1_000_000.0
}

fun Route.modelRoutes(httpClient: HttpClient, apiKey: String) {

    post("/api/models/stream") {
        val request = call.receive<ModelCompareRequest>()

        call.response.header(HttpHeaders.ContentType, "text/event-stream")
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.response.header(HttpHeaders.Connection, "keep-alive")

        call.respondBytesWriter {
            try {
                // Signal all model starts upfront
                for (config in MODEL_TIERS) {
                    writeStringUtf8("data: {\"type\":\"model_start\",\"model\":\"${config.id}\",\"label\":\"${config.label}\"}\n\n")
                }
                flush()

                // Channel to collect results from parallel coroutines
                data class ModelResult(
                    val index: Int,
                    val config: ModelConfig,
                    val content: String,
                    val elapsed: Long,
                    val usage: DeepSeekUsage?,
                    val error: String? = null,
                )

                val results = Channel<ModelResult>(MODEL_TIERS.size)

                coroutineScope {
                    // Launch all 3 API calls in parallel
                    MODEL_TIERS.forEachIndexed { index, config ->
                        launch {
                            val startTime = System.currentTimeMillis()
                            try {
                                val response = httpClient.post(DEEPSEEK_API_URL) {
                                    contentType(ContentType.Application.Json)
                                    bearerAuth(apiKey)
                                    setBody(json.encodeToString(DeepSeekRequest.serializer(),
                                        DeepSeekRequest(
                                            model = config.id,
                                            messages = listOf(DeepSeekMessage(role = "user", content = request.prompt)),
                                            temperature = 0.7,
                                            maxTokens = config.maxTokens,
                                        )
                                    ))
                                }
                                val elapsed = System.currentTimeMillis() - startTime
                                val rawBody = response.bodyAsText()
                                val parsed = json.decodeFromString<DeepSeekFullResponse>(rawBody)
                                val content = parsed.choices.firstOrNull()?.message?.content ?: ""
                                results.send(ModelResult(index, config, content, elapsed, parsed.usage))
                            } catch (e: Exception) {
                                val elapsed = System.currentTimeMillis() - startTime
                                results.send(ModelResult(index, config, "", elapsed, null, e.message))
                            }
                        }
                    }
                }
                results.close()

                // Read results as they arrive and send SSE events
                for (result in results) {
                    if (result.error != null) {
                        val errorMsg = result.error.replace("\"", "\\\"").replace("\n", " ")
                        writeStringUtf8("data: {\"type\":\"error\",\"message\":\"${result.config.label}: $errorMsg\"}\n\n")
                        flush()
                        continue
                    }

                    // Send content
                    val escapedContent = result.content
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                    writeStringUtf8("data: {\"type\":\"model_result\",\"index\":${result.index},\"label\":\"${result.config.label}\",\"content\":\"$escapedContent\"}\n\n")
                    flush()

                    // Send metrics
                    val promptTokens = result.usage?.promptTokens ?: 0
                    val completionTokens = result.usage?.completionTokens ?: 0
                    val totalTokens = result.usage?.totalTokens ?: 0
                    val cost = estimateCost(promptTokens, completionTokens)

                    val metricsJson = json.encodeToString(ModelMetrics.serializer(), ModelMetrics(
                        modelId = result.config.id,
                        modelLabel = result.config.label,
                        responseTimeMs = result.elapsed,
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = totalTokens,
                        estimatedCost = cost,
                    ))
                    writeStringUtf8("data: {\"type\":\"metrics\",\"index\":${result.index},\"data\":$metricsJson}\n\n")
                    flush()
                }

                writeStringUtf8("data: {\"type\":\"done\"}\n\n")
                flush()
            } catch (e: Exception) {
                val errorMsg = (e.message ?: "Unknown error")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                writeStringUtf8("data: {\"type\":\"error\",\"message\":\"$errorMsg\"}\n\n")
                flush()
            }
        }
    }

    post("/api/models/analyze") {
        val request = call.receive<ModelCompareAnalyzeRequest>()
        val resultsText = request.results.joinToString("\n\n") { r ->
            "=== ${r.modelLabel} ===\nResponse time: ${r.responseTimeMs}ms\nTokens: ${r.totalTokens}\nCost: $${String.format("%.6f", r.estimatedCost)}\nContent: ${r.content}"
        }
        val analyzePrompt = """
            |You are an AI assistant comparing different LLM model tiers.
            |Below are responses from 3 model configurations to the same prompt.
            |
            |$resultsText
            |
            |Write a very short comparison (2-3 bullet points, 1 sentence each) about the key differences between these models in terms of quality, speed, and resource efficiency.
            |
            |Respond in this exact JSON format (no markdown, no code blocks):
            |{"comparison":"• bullet1\n• bullet2\n• bullet3"}
        """.trimMargin()

        try {
            val response = httpClient.post(DEEPSEEK_API_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(json.encodeToString(DeepSeekRequest.serializer(),
                    DeepSeekRequest(
                        messages = listOf(DeepSeekMessage(role = "user", content = analyzePrompt)),
                        temperature = 0.0,
                    )
                ))
            }
            val rawBody = response.bodyAsText()
            val deepSeekResponse = json.decodeFromString<DeepSeekResponse>(rawBody)
            val rawContent = deepSeekResponse.choices.firstOrNull()?.message?.content ?: ""
            val analysis = json.decodeFromString<ModelCompareAnalysis>(rawContent)
            call.respond(analysis)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ModelCompareAnalysis(comparison = "Analysis failed: ${e.message}"),
            )
        }
    }
}
