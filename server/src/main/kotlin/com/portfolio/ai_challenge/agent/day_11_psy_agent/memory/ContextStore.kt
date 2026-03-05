package com.portfolio.ai_challenge.agent.day_11_psy_agent.memory

import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyUserProfile

/**
 * Persists and retrieves all conversational data for the Psy-Agent.
 *
 * Owns three layers of memory:
 * - **Turn** — ephemeral, assembled per request via [assembleContext]
 * - **Session** — in-memory conversation history, scoped to one [sessionId]
 * - **Profile** — cross-session user profile, keyed by [userId]
 *
 * Current implementation: [InMemoryContextStore] (in-process ConcurrentHashMap).
 */
interface ContextStore {
    /** Creates a new session linked to [userId] and returns its context. */
    fun createSession(sessionId: String, userId: String): PsySessionContext

    /** Returns the session context or `null` if [sessionId] is unknown. */
    fun loadSession(sessionId: String): PsySessionContext?

    /** Appends a single [ConversationEntry] (user or assistant turn) to the session. */
    fun appendMessage(sessionId: String, entry: ConversationEntry)

    /** Returns the profile for [userId], creating an empty one if none exists. */
    fun loadProfile(userId: String): PsyUserProfile

    /** Overwrites the stored profile (full replace). */
    fun saveProfile(profile: PsyUserProfile)

    /** Merges [newEmotions] into the session's detected-emotions list. */
    fun updateSessionEmotions(sessionId: String, newEmotions: List<String>)

    /**
     * Persists the serialised state string (e.g. `"active_listening:3"`) so it
     * can be restored by [SessionStateMachine.restoreState] on the next request.
     */
    fun updateSessionState(sessionId: String, state: String)

    /**
     * Assembles a [PsyAgentContext] ready for prompt building.
     * Combines profile, recent session messages and [currentState].
     */
    fun assembleContext(sessionId: String, currentState: String): PsyAgentContext
}
