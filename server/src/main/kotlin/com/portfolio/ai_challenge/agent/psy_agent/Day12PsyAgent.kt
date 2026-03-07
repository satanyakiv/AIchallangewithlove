package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

/**
 * Day 12 — Psy-Agent with communication-style personalisation.
 *
 * Route prefix: `/api/agent/psy12/`
 *
 * Extends Day 11 with:
 * - **Personalised system prompt** — [PsyPromptBuilder] injects [PersonalizeResponseUseCase]
 *   to tailor tone, length, and language based on [CommunicationPreferences].
 * - **Profile update feedback** — [PsyChatResult.profileUpdates] lists what was
 *   extracted this turn (name, concern, trigger) for UI display.
 *
 * Preferences are set via `POST /api/agent/psy12/profile/preferences`.
 */
class Day12PsyAgent(
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
        val turnContext = TurnContext(attemptCount = attemptCount, detectedEmotion = detectedEmotion, plan = plan)
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }

        val context = contextStore.assembleContext(sessionId, "active")
        val messages = promptBuilder.buildMessages(context)
        val response = llmClient.complete(messages, maxTokens = 300)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(context.userId)

        val profileUpdateStrings = buildList {
            profileUpdate.preferredName?.let { add("name: $it") }
            profileUpdate.newConcerns.forEach { add("concern: $it") }
            profileUpdate.newTriggers.forEach { add("trigger: $it") }
        }
        return PsyChatResult(response, "active", updatedSession, profile, turnContext, profileUpdateStrings)
    }
}
