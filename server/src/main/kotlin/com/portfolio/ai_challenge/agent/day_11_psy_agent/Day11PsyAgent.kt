package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.MessageRole
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
data class PsyStartRequest(val userId: String)

@Serializable
data class PsyStartResponse(val sessionId: String)

@Serializable
data class PsyChatRequest(val sessionId: String, val message: String)

@Serializable
data class MemoryLayersDebug(
    val turn: String,
    val session: String,
    val profile: String,
)

@Serializable
data class PsyChatResponse(
    val response: String,
    val state: String,
    val memoryLayers: MemoryLayersDebug,
)

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private const val SYSTEM_PROMPT = """You are MindGuard, a compassionate and evidence-based mental health support AI.
Your role is to:
- Listen actively and validate the user's emotions
- Suggest appropriate evidence-based techniques (e.g., Box Breathing, Cognitive Restructuring, Progressive Muscle Relaxation)
- Be warm, non-judgmental, and professional
- Remind users that you are an AI and encourage seeking professional help for serious concerns

Always respond with empathy and care.

Response length rules:
- Maximum 3-4 sentences per response
- Never use numbered lists unless guiding a specific technique step by step
- Never repeat what the user just said back to them in full
- One idea per response. Ask one question at most.
- If suggesting a technique, give only the next single step, not all steps at once"""

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

class PsyAgent(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val contextStore: ContextStore,
    private val contextWindowManager: ContextWindowManager,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResponse {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        // Layer 1 — TurnContext is ephemeral, not stored anywhere
        val turnContext = TurnContext(
            attemptCount = session.messages.count { it.role == MessageRole.USER } + 1,
        )

        // Append user message to Layer 2 (session)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        // Assemble all 3 layers into PsyAgentContext
        val context = contextStore.assembleContext(sessionId, "active")

        // Build context prompt using ContextWindowManager
        val contextPrompt = contextWindowManager.buildPrompt(context)

        // Build messages list for DeepSeek
        val messages = buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = SYSTEM_PROMPT))
            if (contextPrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = "Context:\n$contextPrompt"))
            }
            addAll(context.currentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }

        val response = callDeepSeek(messages)

        // Append assistant reply to Layer 2 (session)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))

        // Build memory debug snapshot for client
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(context.userId)

        val memoryLayers = MemoryLayersDebug(
            turn = "{ plan: ${turnContext.plan}, attemptCount: ${turnContext.attemptCount}, " +
                "detectedEmotion: ${turnContext.detectedEmotion} }",
            session = "{ messageCount: ${updatedSession.messages.size}, " +
                "detectedEmotions: ${updatedSession.detectedEmotions} }",
            profile = "{ userId: ${profile.userId}, preferredName: ${profile.preferredName}, " +
                "concerns: ${profile.primaryConcerns} }",
        )

        return PsyChatResponse(
            response = response,
            state = "active",
            memoryLayers = memoryLayers,
        )
    }

    private suspend fun callDeepSeek(messages: List<DeepSeekMessage>): String {
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = messages,
            temperature = 0.7,
            maxTokens = 300,
        )
        val httpResponse = httpClient.post(DEEPSEEK_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), request))
        }
        val rawBody = httpResponse.bodyAsText()
        if (!httpResponse.status.isSuccess()) {
            throw Exception("DeepSeek error (${httpResponse.status.value}): $rawBody")
        }
        val deepSeekResp = json.decodeFromString<DeepSeekResponse>(rawBody)
        return deepSeekResp.choices.first().message.content
    }
}
