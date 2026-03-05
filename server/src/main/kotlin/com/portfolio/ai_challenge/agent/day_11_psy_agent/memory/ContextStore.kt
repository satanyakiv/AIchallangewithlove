package com.portfolio.ai_challenge.agent.day_11_psy_agent.memory

import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyUserProfile

interface ContextStore {
    fun createSession(sessionId: String, userId: String): PsySessionContext
    fun loadSession(sessionId: String): PsySessionContext?
    fun appendMessage(sessionId: String, entry: ConversationEntry)
    fun loadProfile(userId: String): PsyUserProfile
    fun saveProfile(profile: PsyUserProfile)
    fun updateSessionEmotions(sessionId: String, newEmotions: List<String>)
    fun assembleContext(sessionId: String, currentState: String): PsyAgentContext
}
