package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import kotlinx.serialization.Serializable

@Serializable
data class Day10SlidingRequest(
    val messages: List<ApiMessageDto>,
    val windowSize: Int = 10,
)

@Serializable
data class Day10SlidingResponse(
    val response: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val windowedCount: Int,
    val droppedCount: Int,
)

class Day10SlidingAgent(private val llmClient: LlmClient) {

    suspend fun chat(request: Day10SlidingRequest): Day10SlidingResponse {
        val allMessages = request.messages
        val windowed = allMessages.takeLast(request.windowSize)
        val droppedCount = (allMessages.size - windowed.size).coerceAtLeast(0)

        val messages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Day10Prompts.SYSTEM))
            addAll(windowed.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val deepSeekResp = llmClient.completeWithResponse(messages, temperature = 0.7)
        val usage = deepSeekResp.usage
        return Day10SlidingResponse(
            response = deepSeekResp.choices.first().message.content,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
            windowedCount = windowed.size,
            droppedCount = droppedCount,
        )
    }
}
