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
data class ApiMessageDto(val role: String, val content: String)

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private const val SYSTEM_PROMPT =
    "You are Agent Smith from The Matrix. Speak in his cold, condescending, menacing tone. " +
        "Address the user as 'Mr. Anderson' occasionally. " +
        "Answer questions helpfully but always stay in character."

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Day7Agent(private val httpClient: HttpClient, private val apiKey: String) {

    suspend fun chat(messages: List<ApiMessageDto>): String {
        val allMessages = buildList {
            add(DeepSeekMessage(role = "system", content = SYSTEM_PROMPT))
            addAll(messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = allMessages,
            temperature = 0.7,
        )
        val response = httpClient.post(DEEPSEEK_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), request))
        }
        val rawBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception("DeepSeek error (${response.status.value}): $rawBody")
        }
        val deepSeekResponse = json.decodeFromString<DeepSeekResponse>(rawBody)
        return deepSeekResponse.choices.first().message.content
    }
}
