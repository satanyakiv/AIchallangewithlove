package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import com.portfolio.ai_challenge.models.getOrThrow
import kotlinx.serialization.Serializable

@Serializable
data class ApiMessageDto(val role: MessageRole, val content: String)

class Day7Agent(private val llmClient: LlmClient) {

    suspend fun chat(messages: List<ApiMessageDto>): String {
        val allMessages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Day7.SYSTEM))
            addAll(messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        return llmClient.complete(allMessages).getOrThrow()
    }
}
