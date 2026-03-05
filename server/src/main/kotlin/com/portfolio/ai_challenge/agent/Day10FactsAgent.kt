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
import com.portfolio.ai_challenge.models.MessageRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private const val SYSTEM_PROMPT =
    "You are a helpful AI assistant specializing in product and software requirements. " +
        "Help users define and clarify their project requirements concisely and professionally."

private const val FACTS_EXTRACTION_PROMPT =
    "You are a facts extractor. Analyze the last user-assistant exchange and extract or update key facts.\n" +
        "Return ONLY a valid JSON object with string key-value pairs.\n" +
        "Example: {\"budget\": \"\$80k\", \"platform\": \"iOS and Android\", \"deadline\": \"Q2 2026\"}\n" +
        "Keep existing facts unless updated. Merge with existing. Return only the JSON object, no other text."

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class Day10FactsAgent(private val httpClient: HttpClient, private val apiKey: String) {

    suspend fun chat(request: Day10FactsRequest): Day10FactsResponse {
        // Call 1: extract facts from the latest exchange
        val updatedFacts = extractFacts(
            messages = request.messages,
            existingFacts = request.existingFacts,
        )

        // Call 2: generate answer using facts + messages
        val factsContext = if (updatedFacts.isNotEmpty()) {
            "Known facts about this project:\n" +
                updatedFacts.entries.joinToString("\n") { (k, v) -> "- $k: $v" }
        } else {
            null
        }

        val chatMessages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = SYSTEM_PROMPT))
            if (factsContext != null) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = factsContext))
            }
            addAll(request.messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val chatReq = DeepSeekRequest(
            model = "deepseek-chat",
            messages = chatMessages,
            temperature = 0.7,
        )
        val httpResp = httpClient.post(DEEPSEEK_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), chatReq))
        }
        val rawBody = httpResp.bodyAsText()
        if (!httpResp.status.isSuccess()) {
            throw Exception("DeepSeek error (${httpResp.status.value}): $rawBody")
        }
        val deepSeekResp = json.decodeFromString<DeepSeekResponse>(rawBody)
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
        } else {
            "No existing facts yet."
        }

        val recentExchange = messages.takeLast(2).joinToString("\n") { "${it.role.name}: ${it.content}" }

        val extractionMessages = listOf(
            DeepSeekMessage(role = MessageRole.SYSTEM, content = FACTS_EXTRACTION_PROMPT),
            DeepSeekMessage(
                role = MessageRole.USER,
                content = "$existingJson\n\nLatest exchange:\n$recentExchange",
            ),
        )

        val req = DeepSeekRequest(
            model = "deepseek-chat",
            messages = extractionMessages,
            temperature = 0.1,
        )
        return try {
            val httpResp = httpClient.post(DEEPSEEK_API_URL) {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(DeepSeekRequest.serializer(), req))
            }
            val rawBody = httpResp.bodyAsText()
            if (!httpResp.status.isSuccess()) return existingFacts
            val resp = json.decodeFromString<DeepSeekResponse>(rawBody)
            val rawContent = resp.choices.first().message.content.trim()
            // Strip markdown code blocks if present
            val jsonStr = rawContent
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
            jsonObj.entries.associate { (k, v) -> k to v.jsonPrimitive.content }
        } catch (e: Exception) {
            existingFacts
        }
    }
}
