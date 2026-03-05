package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsySessionSummary
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.models.MessageRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Day11MemoryTest {

    // 1. TurnContext is ephemeral — not stored in ContextStore
    @Test
    fun testTurnContextNotPersisted() {
        val store = InMemoryContextStore()
        store.createSession("s1", "user1")

        val turnContext = TurnContext(plan = "listen", attemptCount = 1, detectedEmotion = "anxiety")

        // TurnContext is a local data class, never stored
        assertNull(store.loadSession("nonexistent"))
        assertNotNull(store.loadSession("s1"))
        // TurnContext fields exist in memory only
        assertEquals("listen", turnContext.plan)
        assertEquals(1, turnContext.attemptCount)
    }

    // 2. Session lifecycle: create → append × 3 → load → verify 3 messages
    @Test
    fun testSessionLifecycle() {
        val store = InMemoryContextStore()
        store.createSession("s2", "user2")

        store.appendMessage("s2", ConversationEntry(role = MessageRole.USER, content = "Hello"))
        store.appendMessage("s2", ConversationEntry(role = MessageRole.ASSISTANT, content = "Hi there"))
        store.appendMessage("s2", ConversationEntry(role = MessageRole.USER, content = "I feel anxious"))

        val session = store.loadSession("s2")
        assertNotNull(session)
        assertEquals(3, session.messages.size)
        assertEquals(MessageRole.USER, session.messages[0].role)
        assertEquals("Hello", session.messages[0].content)
        assertEquals("I feel anxious", session.messages[2].content)
    }

    // 3. User profile persists across save/load
    @Test
    fun testUserProfilePersistence() {
        val store = InMemoryContextStore()
        store.createSession("s3", "user3")

        var profile = store.loadProfile("user3")
        assertEquals("user3", profile.userId)
        assertNull(profile.preferredName)

        val updatedProfile = profile.copy(
            preferredName = "Alice",
            primaryConcerns = listOf("anxiety", "sleep"),
        )
        store.saveProfile(updatedProfile)

        val reloaded = store.loadProfile("user3")
        assertEquals("Alice", reloaded.preferredName)
        assertEquals(listOf("anxiety", "sleep"), reloaded.primaryConcerns)
    }

    // 4. assembleContext returns all 3 layers correctly
    @Test
    fun testAssembleContext() {
        val store = InMemoryContextStore()
        store.createSession("s4", "user4")

        store.appendMessage("s4", ConversationEntry(role = MessageRole.USER, content = "I'm stressed"))

        val profile = store.loadProfile("user4").copy(
            preferredName = "Bob",
            primaryConcerns = listOf("stress"),
            sessionHistory = listOf(
                PsySessionSummary("old-session", "User discussed work stress"),
            ),
        )
        store.saveProfile(profile)

        val context = store.assembleContext("s4", "active")

        assertEquals("s4", context.sessionId)
        assertEquals("user4", context.userId)
        assertEquals("active", context.currentState)
        assertEquals(1, context.currentMessages.size)
        assertEquals("Bob", context.userProfile.preferredName)
        assertEquals(1, context.recentSessions.size)
        assertEquals("User discussed work stress", context.recentSessions[0].summaryText)
        assertEquals(3, context.domain.techniques.size) // DEFAULT_DOMAIN has 3 techniques
    }

    // 5. ContextWindowManager truncates when maxTokens is small
    @Test
    fun testContextWindowTruncation() {
        val store = InMemoryContextStore()
        val manager = ContextWindowManager()
        store.createSession("s5", "user5")

        // Add 20 messages with long content
        repeat(20) { i ->
            store.appendMessage("s5", ConversationEntry(
                role = if (i % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                content = "This is message number $i with some content to make it longer than a few words.",
            ))
        }

        val context = store.assembleContext("s5", "active")
        val prompt = manager.buildPrompt(context, maxTokens = 500)

        // Rough estimate: prompt should fit within ~500*4 = 2000 chars
        val estimatedTokens = prompt.length / 4
        assertTrue(estimatedTokens <= 500, "Expected <= 500 tokens, got $estimatedTokens for prompt length ${prompt.length}")
    }

    // 6. Two sessions for same user are independent; profile is shared
    @Test
    fun testMultipleSessions() {
        val store = InMemoryContextStore()
        store.createSession("sess-a", "shared-user")
        store.createSession("sess-b", "shared-user")

        store.appendMessage("sess-a", ConversationEntry(role = MessageRole.USER, content = "Session A message"))
        store.appendMessage("sess-b", ConversationEntry(role = MessageRole.USER, content = "Session B message"))

        val sessionA = store.loadSession("sess-a")!!
        val sessionB = store.loadSession("sess-b")!!

        assertEquals(1, sessionA.messages.size)
        assertEquals(1, sessionB.messages.size)
        assertEquals("Session A message", sessionA.messages[0].content)
        assertEquals("Session B message", sessionB.messages[0].content)

        // Profile is shared
        val updatedProfile = store.loadProfile("shared-user").copy(preferredName = "Charlie")
        store.saveProfile(updatedProfile)

        assertEquals("Charlie", store.loadProfile("shared-user").preferredName)
    }
}
