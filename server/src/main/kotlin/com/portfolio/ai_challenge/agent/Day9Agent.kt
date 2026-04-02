package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import com.portfolio.ai_challenge.models.getOrThrow
import kotlinx.serialization.Serializable

@Serializable
data class Day9ChatRequest(
    val recentMessages: List<ApiMessageDto>,
    val oldMessages: List<ApiMessageDto>,
    val existingSummary: String?,
    val compressionEnabled: Boolean,
)

@Serializable
data class Day9ChatResponse(
    val response: String,
    val newSummary: String?,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

class Day9Agent(private val llmClient: LlmClient) {

    suspend fun chat(request: Day9ChatRequest): Day9ChatResponse {
        return if (request.compressionEnabled && request.oldMessages.isNotEmpty()) {
            chatCompressed(request.recentMessages, request.oldMessages, request.existingSummary)
        } else {
            chatNormal(request.recentMessages + request.oldMessages)
        }
    }

    private suspend fun chatNormal(allMessages: List<ApiMessageDto>): Day9ChatResponse {
        val messages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Day9.SYSTEM))
            addAll(allMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val deepSeekResponse = llmClient.completeWithResponse(messages, temperature = 0.7).getOrThrow()
        val usage = deepSeekResponse.usage
        return Day9ChatResponse(
            response = deepSeekResponse.choices.first().message.content,
            newSummary = null,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }

    private suspend fun chatCompressed(
        recentMessages: List<ApiMessageDto>,
        oldMessages: List<ApiMessageDto>,
        existingSummary: String?,
    ): Day9ChatResponse {
        val summaryUserContent = buildString {
            if (existingSummary != null) {
                appendLine("Existing summary of earlier conversation:")
                appendLine(existingSummary)
                appendLine()
                appendLine("Additional messages to incorporate into the summary:")
            } else {
                appendLine("Conversation to summarize:")
            }
            oldMessages.forEach { msg -> appendLine("${msg.role.displayName}: ${msg.content}") }
        }

        val summaryMessages = listOf(
            DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Day9.SUMMARY_SYSTEM),
            DeepSeekMessage(role = MessageRole.USER, content = summaryUserContent),
        )
        val summaryResponse = llmClient.completeWithResponse(summaryMessages, temperature = 0.3).getOrThrow()
        val newSummary = summaryResponse.choices.first().message.content

        val compressedMessages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Day9.SYSTEM))
            add(DeepSeekMessage(
                role = MessageRole.SYSTEM,
                content = "Previous conversation summary (for context): $newSummary",
            ))
            addAll(recentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val answerResponse = llmClient.completeWithResponse(compressedMessages, temperature = 0.7).getOrThrow()
        val usage = answerResponse.usage

        return Day9ChatResponse(
            response = answerResponse.choices.first().message.content,
            newSummary = newSummary,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }
}
