package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import kotlinx.serialization.Serializable

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

class Day10BranchingAgent(private val llmClient: LlmClient) {

    suspend fun chat(request: Day10BranchingRequest): Day10BranchingResponse {
        val messages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Day10.BRANCHING))
            addAll(request.messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val deepSeekResp = llmClient.completeWithResponse(messages, temperature = 0.7)
        val usage = deepSeekResp.usage
        return Day10BranchingResponse(
            response = deepSeekResp.choices.first().message.content,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }
}
