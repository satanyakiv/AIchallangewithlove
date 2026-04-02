package com.portfolio.ai_challenge.models

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * Thin HTTP wrapper around the DeepSeek Chat Completions API.
 *
 * Returns [Result] instead of throwing — callers decide how to handle errors.
 * Legacy agents can use [getOrThrow] for backward compatibility.
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
    ): Result<String, LlmError> =
        completeWithResponse(messages, temperature, maxTokens)
            .map { it.choices.first().message.content }

    suspend fun completeWithResponse(
        messages: List<DeepSeekMessage>,
        temperature: Double = 0.7,
        maxTokens: Int? = null,
    ): Result<DeepSeekResponse, LlmError> {
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
            logger.warn { "DeepSeek error (${httpResponse.status.value}): $rawBody" }
            return Err(LlmError.HttpError(httpResponse.status.value, rawBody))
        }
        return try {
            Ok(json.decodeFromString<DeepSeekResponse>(rawBody))
        } catch (e: Exception) {
            Err(LlmError.ParseError(e))
        }
    }
}
