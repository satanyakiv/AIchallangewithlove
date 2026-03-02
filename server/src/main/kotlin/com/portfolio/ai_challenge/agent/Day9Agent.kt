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

// Request from client → server
@Serializable
data class Day9ChatRequest(
    val recentMessages: List<ApiMessageDto>,     // last N messages, always sent
    val oldMessages: List<ApiMessageDto>,        // older messages (empty if compression=false)
    val existingSummary: String?,                // previously stored summary (null if none)
    val compressionEnabled: Boolean,
)

// Response from server → client
@Serializable
data class Day9ChatResponse(
    val response: String,
    val newSummary: String?,                     // null if compression=false
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private const val SYSTEM_PROMPT =
    "You are Agent Smith from The Matrix. Speak in his cold, condescending, menacing tone. " +
        "Address the user as 'Mr. Anderson' occasionally. " +
        "Answer questions helpfully but always stay in character."

private const val SUMMARY_SYSTEM_PROMPT =
    "You are a context summarizer. Create a concise summary of the conversation history provided. " +
        "Focus on key topics, facts, and context that would be needed to continue the conversation. " +
        "Be brief but comprehensive. Output only the summary text, no preamble."

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Day9Agent(private val httpClient: HttpClient, private val apiKey: String) {

    suspend fun chat(request: Day9ChatRequest): Day9ChatResponse {
        return if (request.compressionEnabled && request.oldMessages.isNotEmpty()) {
            chatCompressed(request.recentMessages, request.oldMessages, request.existingSummary)
        } else {
            chatNormal(request.recentMessages + request.oldMessages)
        }
    }

    // Normal path: send ALL messages to DeepSeek unchanged
    private suspend fun chatNormal(allMessages: List<ApiMessageDto>): Day9ChatResponse {
        val messages = buildList {
            add(DeepSeekMessage(role = "system", content = SYSTEM_PROMPT))
            addAll(allMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val deepSeekResponse = callDeepSeek(messages, temperature = 0.7)
        val usage = deepSeekResponse.usage
        return Day9ChatResponse(
            response = deepSeekResponse.choices.first().message.content,
            newSummary = null,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }

    // Compressed path: 2 DeepSeek calls
    // Call 1: summarize old messages
    // Call 2: answer using summary + recent messages only
    private suspend fun chatCompressed(
        recentMessages: List<ApiMessageDto>,
        oldMessages: List<ApiMessageDto>,
        existingSummary: String?,
    ): Day9ChatResponse {
        // Build summarization prompt
        val summaryUserContent = buildString {
            if (existingSummary != null) {
                appendLine("Existing summary of earlier conversation:")
                appendLine(existingSummary)
                appendLine()
                appendLine("Additional messages to incorporate into the summary:")
            } else {
                appendLine("Conversation to summarize:")
            }
            oldMessages.forEach { msg ->
                appendLine("${msg.role.uppercase()}: ${msg.content}")
            }
        }

        // Call 1: get summary of old messages
        val summaryMessages = listOf(
            DeepSeekMessage(role = "system", content = SUMMARY_SYSTEM_PROMPT),
            DeepSeekMessage(role = "user", content = summaryUserContent),
        )
        val summaryResponse = callDeepSeek(summaryMessages, temperature = 0.3)
        val newSummary = summaryResponse.choices.first().message.content

        // Call 2: answer with compressed context
        val compressedMessages = buildList {
            add(DeepSeekMessage(role = "system", content = SYSTEM_PROMPT))
            // Inject summary as a system-level context note
            add(
                DeepSeekMessage(
                    role = "system",
                    content = "Previous conversation summary (for context): $newSummary",
                )
            )
            // Append only recent messages
            addAll(recentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val answerResponse = callDeepSeek(compressedMessages, temperature = 0.7)
        val usage = answerResponse.usage

        return Day9ChatResponse(
            response = answerResponse.choices.first().message.content,
            newSummary = newSummary,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }

    private suspend fun callDeepSeek(
        messages: List<DeepSeekMessage>,
        temperature: Double,
    ): DeepSeekResponse {
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = messages,
            temperature = temperature,
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
        return json.decodeFromString<DeepSeekResponse>(rawBody)
    }
}
