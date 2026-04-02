package com.portfolio.ai_challenge.agent.psy_agent.memory

import io.github.oshai.kotlinlogging.KotlinLogging
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.DEFAULT_DOMAIN
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyUserProfile
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

class InMemoryContextStore : ContextStore {

    private val sessions = ConcurrentHashMap<String, PsySessionContext>()
    private val profiles = ConcurrentHashMap<String, PsyUserProfile>()
    private val taskPhases = ConcurrentHashMap<String, String>()

    override fun createSession(sessionId: String, userId: String): PsySessionContext {
        val session = PsySessionContext(sessionId = sessionId, userId = userId)
        sessions[sessionId] = session
        profiles.getOrPut(userId) { PsyUserProfile(userId = userId) }
        logger.debug { "Session created: $sessionId for user $userId" }
        return session
    }

    override fun loadSession(sessionId: String): PsySessionContext? = sessions[sessionId]

    override fun appendMessage(sessionId: String, entry: ConversationEntry) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(messages = current.messages + entry)
    }

    override fun loadProfile(userId: String): PsyUserProfile =
        profiles.getOrPut(userId) { PsyUserProfile(userId = userId) }

    override fun saveProfile(profile: PsyUserProfile) {
        profiles[profile.userId] = profile
    }

    override fun updateSessionEmotions(sessionId: String, newEmotions: List<String>) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(
            detectedEmotions = (current.detectedEmotions + newEmotions).distinct(),
        )
    }

    override fun updateTaskPhase(sessionId: String, phase: String) {
        taskPhases[sessionId] = phase
    }

    override fun loadTaskPhase(sessionId: String): String? = taskPhases[sessionId]

    override fun updateSessionState(sessionId: String, state: String) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(currentState = state)
    }

    override fun assembleContext(sessionId: String, currentState: String): PsyAgentContext {
        val session = sessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val profile = loadProfile(session.userId)
        val recentSessions = profile.sessionHistory.takeLast(3)
        return PsyAgentContext(
            sessionId = sessionId,
            userId = session.userId,
            currentState = currentState,
            currentMessages = session.messages,
            userProfile = profile,
            recentSessions = recentSessions,
            domain = DEFAULT_DOMAIN,
        )
    }
}
