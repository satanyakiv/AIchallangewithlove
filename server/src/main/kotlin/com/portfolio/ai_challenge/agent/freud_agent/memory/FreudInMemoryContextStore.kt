package com.portfolio.ai_challenge.agent.freud_agent.memory

import com.portfolio.ai_challenge.agent.freud_agent.model.FreudAgentContext
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudUserProfile
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import java.util.concurrent.ConcurrentHashMap

class FreudInMemoryContextStore : FreudContextStore {

    private val sessions = ConcurrentHashMap<String, PsySessionContext>()
    private val profiles = ConcurrentHashMap<String, FreudUserProfile>()

    override fun createSession(sessionId: String, userId: String): PsySessionContext {
        val session = PsySessionContext(
            sessionId = sessionId,
            userId = userId,
            currentState = FreudSessionState.Begruessung.displayName,
        )
        sessions[sessionId] = session
        profiles.getOrPut(userId) { FreudUserProfile(userId = userId) }
        return session
    }

    override fun loadSession(sessionId: String): PsySessionContext? = sessions[sessionId]

    override fun appendMessage(sessionId: String, entry: ConversationEntry) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(messages = current.messages + entry)
    }

    override fun loadProfile(userId: String): FreudUserProfile =
        profiles.getOrPut(userId) { FreudUserProfile(userId = userId) }

    override fun saveProfile(profile: FreudUserProfile) {
        profiles[profile.userId] = profile
    }

    override fun updateSessionState(sessionId: String, state: String) {
        val current = sessions[sessionId] ?: return
        sessions[sessionId] = current.copy(currentState = state)
    }

    override fun assembleContext(sessionId: String, currentState: String): FreudAgentContext {
        val session = sessions[sessionId]
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        val profile = loadProfile(session.userId)
        return FreudAgentContext(
            sessionId = sessionId,
            userId = session.userId,
            currentState = currentState,
            currentMessages = session.messages,
            userProfile = profile,
        )
    }
}
