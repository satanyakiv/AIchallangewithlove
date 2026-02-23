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
import kotlinx.serialization.json.Json

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun Route.temperatureRoutes(httpClient: HttpClient, apiKey: String) {

    post("/api/temperature/stream") {
        val request = call.receive<TemperatureRequest>()

        call.response.header(HttpHeaders.ContentType, "text/event-stream")
        call.response.header(HttpHeaders.CacheControl, "no-cache")
        call.response.header(HttpHeaders.Connection, "keep-alive")

        call.respondBytesWriter {
            try {
                httpClient.preparePost(DEEPSEEK_API_URL) {
                    contentType(ContentType.Application.Json)
                    bearerAuth(apiKey)
                    setBody(json.encodeToString(DeepSeekRequest.serializer(),
                        DeepSeekRequest(
                            messages = listOf(DeepSeekMessage(role = "user", content = request.prompt)),
                            temperature = request.temperature,
                            stream = true,
                        )
                    ))
                }.execute { response ->
                    if (response.status != HttpStatusCode.OK) {
                        val errorBody = response.bodyAsText()
                        writeStringUtf8("data: {\"error\":\"DeepSeek API error: ${response.status} - $errorBody\"}\n\n")
                        flush()
                        return@execute
                    }

                    val channel = response.bodyAsChannel()
                    val buffer = StringBuilder()

                    while (!channel.isClosedForRead) {
                        val line = try {
                            channel.readUTF8Line()
                        } catch (e: Exception) {
                            null
                        } ?: break

                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                writeStringUtf8("data: [DONE]\n\n")
                                flush()
                                break
                            }
                            try {
                                val chunk = json.decodeFromString<DeepSeekStreamChunk>(data)
                                val content = chunk.choices.firstOrNull()?.delta?.content
                                if (content != null) {
                                    val escaped = content
                                        .replace("\\", "\\\\")
                                        .replace("\"", "\\\"")
                                        .replace("\n", "\\n")
                                    writeStringUtf8("data: {\"content\":\"$escaped\"}\n\n")
                                    flush()
                                }
                            } catch (_: Exception) {
                                // skip unparseable chunks
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = (e.message ?: "Unknown error")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                writeStringUtf8("data: {\"error\":\"$errorMsg\"}\n\n")
                flush()
            }
        }
    }

    post("/api/temperature") {
        val request = call.receive<TemperatureRequest>()
        try {
            val response = httpClient.post(DEEPSEEK_API_URL) {
                contentType(ContentType.Application.Json)
                bearerAuth(apiKey)
                setBody(json.encodeToString(DeepSeekRequest.serializer(),
                    DeepSeekRequest(
                        messages = listOf(DeepSeekMessage(role = "user", content = request.prompt)),
                        temperature = request.temperature,
                    )
                ))
            }
            val rawBody = response.bodyAsText()
            val body = json.decodeFromString<DeepSeekResponse>(rawBody)
            val content = body.choices.firstOrNull()?.message?.content ?: "No response"
            call.respond(TemperatureResponse(temperature = request.temperature, content = content))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                TemperatureResponse(temperature = request.temperature, content = "", error = e.message),
            )
        }
    }

    post("/api/temperature/analyze") {
        val request = call.receive<AnalyzeRequest>()
        val resultsText = request.results.joinToString("\n\n") { result ->
            "=== Temperature ${result.temperature} ===\n${result.content}"
        }
        val analyzePrompt = """
            |You are an AI assistant analyzing the effect of the temperature parameter on LLM outputs.
            |Below are 3 responses to the same prompt, generated with different temperature values.
            |
            |$resultsText
            |
            |Tasks:
            |1. Compare these 3 responses by: accuracy, creativity, and diversity. Write a brief comparison (3-5 sentences).
            |2. For each temperature (0, 0.7, 1.2), write 1-2 sentences about which software development tasks it's best suited for.
            |
            |Respond in this exact JSON format (no markdown, no code blocks):
            |{"comparison":"...","recommendations":[{"temperature":0.0,"bestFor":"..."},{"temperature":0.7,"bestFor":"..."},{"temperature":1.2,"bestFor":"..."}]}
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
            val analyzeResponse = json.decodeFromString<AnalyzeResponse>(rawContent)
            call.respond(analyzeResponse)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                AnalyzeResponse(
                    comparison = "Analysis failed: ${e.message}",
                    recommendations = emptyList(),
                ),
            )
        }
    }
}
