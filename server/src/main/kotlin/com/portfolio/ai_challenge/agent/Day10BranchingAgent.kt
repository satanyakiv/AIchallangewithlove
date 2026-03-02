package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.DeepSeekRequest
import com.portfolio.ai_challenge.models.DeepSeekResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Day10BranchingRequest(
    val messages: List<ApiMessageDto>,
)

@Serializable
data class Day10BranchingResponse(
    val response: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
private const val SYSTEM_PROMPT =
    "You are a helpful AI assistant specializing in product and software requirements. " +
        "Help users define and clarify their project requirements concisely and professionally."

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Day10BranchingAgent(private val httpClient: HttpClient, private val apiKey: String) {

    suspend fun chat(request: Day10BranchingRequest): Day10BranchingResponse {
        val messages = buildList {
            add(DeepSeekMessage(role = "system", content = SYSTEM_PROMPT))
            addAll(request.messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val deepSeekReq = DeepSeekRequest(
            model = "deepseek-chat",
            messages = messages,
            temperature = 0.7,
        )
        val httpResponse = httpClient.post(DEEPSEEK_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), deepSeekReq))
        }
        val rawBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            throw Exception("DeepSeek error (${httpResponse.status.value}): $rawBody")
        }
        val deepSeekResp = json.decodeFromString<DeepSeekResponse>(rawBody)
        val usage = deepSeekResp.usage
        return Day10BranchingResponse(
            response = deepSeekResp.choices.first().message.content,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }
}
