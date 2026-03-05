package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

class PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val detectedEmotion = profileUpdate.newConcerns.firstOrNull()
        val plan = if (detectedEmotion != null) "validate_and_explore" else "active_listening"
        val turnContext = TurnContext(
            attemptCount = attemptCount,
            detectedEmotion = detectedEmotion,
            plan = plan,
        )
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }

        val context = contextStore.assembleContext(sessionId, "active")
        val messages = promptBuilder.buildMessages(context)
        val response = llmClient.complete(messages, maxTokens = 300)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(context.userId)
        return PsyChatResult(response, "active", updatedSession, profile, turnContext)
    }
}
