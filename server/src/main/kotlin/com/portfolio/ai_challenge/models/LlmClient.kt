package com.portfolio.ai_challenge.models

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Thin HTTP wrapper around the DeepSeek Chat Completions API.
 *
 * Shared by all agents (Day 6–13). Uses `bodyAsText()` + manual JSON
 * because the Ktor HttpClient is not configured with ContentNegotiation.
 *
 * @param httpClient Ktor [HttpClient] with CIO engine.
 * @param apiKey DeepSeek API key (read from `DEEPSEEK_API_KEY` env var).
 * @param apiUrl Completions endpoint. Default: DeepSeek production.
 * @param model Model identifier. Default: `deepseek-chat` (V3).
 */
class LlmClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val apiUrl: String = "https://api.deepseek.com/chat/completions",
    private val model: String = "deepseek-chat",
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun complete(
        messages: List<DeepSeekMessage>,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
    ): String = completeWithResponse(messages, temperature, maxTokens).choices.first().message.content

    suspend fun completeWithResponse(
        messages: List<DeepSeekMessage>,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
    ): DeepSeekResponse {
        val request = DeepSeekRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
        )
        val httpResponse = httpClient.post(apiUrl) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), request))
        }
        val rawBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            throw Exception("DeepSeek error (${httpResponse.status.value}): $rawBody")
        }
        return json.decodeFromString<DeepSeekResponse>(rawBody)
    }
}
