package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Day10FactsRequest(
    val messages: List<ApiMessageDto>,
    val existingFacts: Map<String, String> = emptyMap(),
)

@Serializable
data class Day10FactsResponse(
    val response: String,
    val updatedFacts: Map<String, String>,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Day10FactsAgent(private val llmClient: LlmClient) {

    suspend fun chat(request: Day10FactsRequest): Day10FactsResponse {
        val updatedFacts = extractFacts(request.messages, request.existingFacts)

        val factsContext = if (updatedFacts.isNotEmpty()) {
            "Known facts about this project:\n" +
                updatedFacts.entries.joinToString("\n") { (k, v) -> "- $k: $v" }
        } else null

        val chatMessages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Day10Prompts.SYSTEM))
            if (factsContext != null) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = factsContext))
            }
            addAll(request.messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val deepSeekResp = llmClient.completeWithResponse(chatMessages, temperature = 0.7)
        val usage = deepSeekResp.usage
        return Day10FactsResponse(
            response = deepSeekResp.choices.first().message.content,
            updatedFacts = updatedFacts,
            promptTokens = usage?.promptTokens ?: 0,
            completionTokens = usage?.completionTokens ?: 0,
            totalTokens = usage?.totalTokens ?: 0,
        )
    }

    private suspend fun extractFacts(
        messages: List<ApiMessageDto>,
        existingFacts: Map<String, String>,
    ): Map<String, String> {
        if (messages.isEmpty()) return existingFacts

        val existingJson = if (existingFacts.isNotEmpty()) {
            "Existing facts: " + existingFacts.entries.joinToString(", ") { "\"${it.key}\": \"${it.value}\"" }
        } else "No existing facts yet."

        val recentExchange = messages.takeLast(2).joinToString("\n") { "${it.role.name}: ${it.content}" }

        val extractionMessages = listOf(
            DeepSeekMessage(role = MessageRole.SYSTEM, content = Day10Prompts.FACTS_EXTRACTION),
            DeepSeekMessage(role = MessageRole.USER, content = "$existingJson\n\nLatest exchange:\n$recentExchange"),
        )

        return try {
            val rawContent = llmClient.complete(extractionMessages, temperature = 0.1)
            val jsonStr = rawContent.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
            jsonObj.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
        } catch (e: Exception) {
            existingFacts
        }
    }
}
