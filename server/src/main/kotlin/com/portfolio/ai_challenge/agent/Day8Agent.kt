package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.DeepSeekRequest
import com.portfolio.ai_challenge.models.DeepSeekResponse
import com.portfolio.ai_challenge.models.TokenUsage
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class AgentResponse(
    val content: String,
    val usage: TokenUsage?,
    val rawResponseBody: String,
    val httpStatus: Int,
    val errorMessage: String? = null,
)

class Day8Agent(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val systemPrompt: String,
) {
    suspend fun chat(messages: List<ApiMessageDto>): AgentResponse {
        val allMessages = buildList {
            add(DeepSeekMessage(role = "system", content = systemPrompt))
            addAll(messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = allMessages,
            temperature = 0.3,
        )
        return try {
            val response = httpClient.post(DEEPSEEK_API_URL) {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(DeepSeekRequest.serializer(), request))
            }
            val rawBody = response.bodyAsText()
            val statusCode = response.status.value
            if (!response.status.value.let { it in 200..299 }) {
                AgentResponse(
                    content = "",
                    usage = null,
                    rawResponseBody = rawBody,
                    httpStatus = statusCode,
                    errorMessage = "DeepSeek error ($statusCode): $rawBody",
                )
            } else {
                val deepSeekResponse = json.decodeFromString<DeepSeekResponse>(rawBody)
                AgentResponse(
                    content = deepSeekResponse.choices.firstOrNull()?.message?.content ?: "",
                    usage = deepSeekResponse.usage,
                    rawResponseBody = rawBody,
                    httpStatus = statusCode,
                )
            }
        } catch (e: Exception) {
            AgentResponse(
                content = "",
                usage = null,
                rawResponseBody = "",
                httpStatus = 0,
                errorMessage = "Network error: ${e.message}",
            )
        }
    }
}
