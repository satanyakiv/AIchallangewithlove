package com.portfolio.ai_challenge.agent.freud_agent.memory

import com.portfolio.ai_challenge.agent.freud_agent.model.FreudAgentContext
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudUserProfile
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext

interface FreudContextStore {
    fun createSession(sessionId: String, userId: String): PsySessionContext
    fun loadSession(sessionId: String): PsySessionContext?
    fun appendMessage(sessionId: String, entry: ConversationEntry)
    fun loadProfile(userId: String): FreudUserProfile
    fun saveProfile(profile: FreudUserProfile)
    fun updateSessionState(sessionId: String, state: String)
    fun assembleContext(sessionId: String, currentState: String): FreudAgentContext
}
