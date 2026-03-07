package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.psy_agent.PersonalizeResponseUseCase
import com.portfolio.ai_challenge.agent.psy_agent.ProfileExtractor
import com.portfolio.ai_challenge.agent.psy_agent.PsyAgent
import com.portfolio.ai_challenge.agent.psy_agent.UpdateProfileUseCase
import com.portfolio.ai_challenge.agent.psy_agent.PsyPromptBuilder
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionSummary
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Day11PersistenceTest {

    private val contextStore = InMemoryContextStore()
    private val llmClient = mockk<LlmClient>()
    private val agent = PsyAgent(
        contextStore = contextStore,
        promptBuilder = PsyPromptBuilder(ContextWindowManager(), PersonalizeResponseUseCase()),
        llmClient = llmClient,
        updateProfile = UpdateProfileUseCase(ProfileExtractor(), contextStore),
    )

    init {
        coEvery { llmClient.complete(any(), any(), any()) } returns "I hear you."
    }

    // ─── Session lifecycle ────────────────────────────────────────────────────

    @Test
    fun testCreateSession_returnsValidSessionId() {
        val sessionId = agent.startSession("user1")
        assertTrue(sessionId.isNotBlank())
    }

    @Test
    fun testAppendMessage_messagesInCorrectOrder() {
        val sessionId = contextStore.run {
            createSession("s-order", "user-order")
            "s-order"
        }
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = "first"))
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = "second"))
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = "third"))

        val session = contextStore.loadSession(sessionId)
        assertNotNull(session)
        assertEquals(3, session.messages.size)
        assertEquals("first", session.messages[0].content)
        assertEquals("second", session.messages[1].content)
        assertEquals("third", session.messages[2].content)
    }

    @Test
    fun testInvalidSessionId_throwsException() = runTest {
        var threw = false
        try {
            agent.chat("fake-id-that-does-not-exist", "hello")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw, "Expected IllegalArgumentException for unknown sessionId")
    }

    @Test
    fun testTwoSessions_sameUser_isolatedMessages() = runTest {
        val s1 = agent.startSession("shared-user-iso")
        val s2 = agent.startSession("shared-user-iso")

        agent.chat(s1, "hello from session one")
        agent.chat(s2, "goodbye from session two")

        val session1 = contextStore.loadSession(s1)!!
        val session2 = contextStore.loadSession(s2)!!

        assertTrue(session1.messages.any { it.content == "hello from session one" })
        assertFalse(session1.messages.any { it.content == "goodbye from session two" })
        assertTrue(session2.messages.any { it.content == "goodbye from session two" })
        assertFalse(session2.messages.any { it.content == "hello from session one" })
    }

    // ─── Profile persistence ──────────────────────────────────────────────────

    @Test
    fun testPreferredName_userSaysName_profileUpdated() = runTest {
        val sid = agent.startSession("user-name-1")
        agent.chat(sid, "My name is Katya")
        val profile = contextStore.loadProfile("user-name-1")
        assertEquals("Katya", profile.preferredName)
    }

    @Test
    fun testPreferredName_callMe_profileUpdated() = runTest {
        val sid = agent.startSession("user-name-2")
        agent.chat(sid, "Please call me Dmytro")
        val profile = contextStore.loadProfile("user-name-2")
        assertEquals("Dmytro", profile.preferredName)
    }

    @Test
    fun testPreferredName_persistsAcrossSessions() = runTest {
        val userId = "user-name-persist"
        val s1 = agent.startSession(userId)
        agent.chat(s1, "My name is Katya")

        val s2 = agent.startSession(userId)
        agent.chat(s2, "Hello again")

        val profile = contextStore.loadProfile(userId)
        assertEquals("Katya", profile.preferredName)
    }

    @Test
    fun testConcerns_anxietyKeyword_extracted() = runTest {
        val sid = agent.startSession("user-concerns-1")
        agent.chat(sid, "I have been feeling really anxious lately")
        val profile = contextStore.loadProfile("user-concerns-1")
        assertTrue("anxiety" in profile.primaryConcerns, "Expected 'anxiety' in ${profile.primaryConcerns}")
    }

    @Test
    fun testConcerns_sleepKeyword_extracted() = runTest {
        val sid = agent.startSession("user-concerns-2")
        agent.chat(sid, "I cant sleep at night")
        val profile = contextStore.loadProfile("user-concerns-2")
        assertTrue("sleep issues" in profile.primaryConcerns, "Expected 'sleep issues' in ${profile.primaryConcerns}")
    }

    @Test
    fun testConcerns_accumulateAcrossMessages() = runTest {
        val sid = agent.startSession("user-concerns-3")
        agent.chat(sid, "I feel so anxious all the time")
        agent.chat(sid, "And I cant sleep either")
        val profile = contextStore.loadProfile("user-concerns-3")
        assertTrue("anxiety" in profile.primaryConcerns)
        assertTrue("sleep issues" in profile.primaryConcerns)
    }

    @Test
    fun testConcerns_noDuplicates() = runTest {
        val sid = agent.startSession("user-concerns-4")
        agent.chat(sid, "I feel so anxious")
        agent.chat(sid, "I am really anxious again")
        val profile = contextStore.loadProfile("user-concerns-4")
        assertEquals(1, profile.primaryConcerns.count { it == "anxiety" },
            "Expected 'anxiety' to appear exactly once, got: ${profile.primaryConcerns}")
    }

    @Test
    fun testTriggers_workKeyword_extracted() = runTest {
        val sid = agent.startSession("user-triggers-1")
        agent.chat(sid, "Work deadlines are killing me")
        val profile = contextStore.loadProfile("user-triggers-1")
        assertTrue("work stress" in profile.knownTriggers, "Expected 'work stress' in ${profile.knownTriggers}")
    }

    @Test
    fun testTriggers_familyKeyword_extracted() = runTest {
        val sid = agent.startSession("user-triggers-2")
        agent.chat(sid, "Having issues with my partner")
        val profile = contextStore.loadProfile("user-triggers-2")
        assertTrue("family dynamics" in profile.knownTriggers, "Expected 'family dynamics' in ${profile.knownTriggers}")
    }

    @Test
    fun testTriggers_persistAcrossSessions() = runTest {
        val userId = "user-triggers-persist"
        val s1 = agent.startSession(userId)
        agent.chat(s1, "My boss is so stressful")

        val s2 = agent.startSession(userId)
        agent.chat(s2, "How are you?")

        val profile = contextStore.loadProfile(userId)
        assertTrue("work stress" in profile.knownTriggers)
    }

    @Test
    fun testProfileNotOverwritten_onNewSession() = runTest {
        val userId = "user-no-overwrite"
        val s1 = agent.startSession(userId)
        agent.chat(s1, "My name is Katya and I feel anxious")

        val s2 = agent.startSession(userId)
        agent.chat(s2, "Hello")

        val profile = contextStore.loadProfile(userId)
        assertEquals("Katya", profile.preferredName)
        assertTrue("anxiety" in profile.primaryConcerns)
    }

    @Test
    fun testTwoUsers_separateProfiles() = runTest {
        val sA = agent.startSession("user-a")
        val sB = agent.startSession("user-b")

        agent.chat(sA, "My name is Katya")
        agent.chat(sB, "My name is Dmytro")

        assertEquals("Katya", contextStore.loadProfile("user-a").preferredName)
        assertEquals("Dmytro", contextStore.loadProfile("user-b").preferredName)
    }

    // ─── Context assembly ─────────────────────────────────────────────────────

    @Test
    fun testAssembleContext_returnsCurrentSessionMessages() {
        val sessionId = "s-ctx-1"
        contextStore.createSession(sessionId, "user-ctx-1")
        contextStore.appendMessage(sessionId, ConversationEntry(MessageRole.USER, "a"))
        contextStore.appendMessage(sessionId, ConversationEntry(MessageRole.ASSISTANT, "b"))
        contextStore.appendMessage(sessionId, ConversationEntry(MessageRole.USER, "c"))

        val ctx = contextStore.assembleContext(sessionId, "active")
        assertEquals(3, ctx.currentMessages.size)
    }

    @Test
    fun testAssembleContext_includesUserProfile() {
        val sessionId = "s-ctx-2"
        contextStore.createSession(sessionId, "user-ctx-2")
        val profile = contextStore.loadProfile("user-ctx-2").copy(
            primaryConcerns = listOf("anxiety", "sleep issues"),
        )
        contextStore.saveProfile(profile)

        val ctx = contextStore.assembleContext(sessionId, "active")
        assertEquals(listOf("anxiety", "sleep issues"), ctx.userProfile.primaryConcerns)
    }

    @Test
    fun testAssembleContext_emptyProfile_doesNotCrash() {
        val sessionId = "s-ctx-3"
        contextStore.createSession(sessionId, "user-ctx-3-new")

        val ctx = contextStore.assembleContext(sessionId, "active")
        assertNotNull(ctx)
        assertEquals("user-ctx-3-new", ctx.userId)
    }

    @Test
    fun testAssembleContext_includesRecentSessionHistory() {
        val sessionId = "s-ctx-4"
        contextStore.createSession(sessionId, "user-ctx-4")
        val profile = contextStore.loadProfile("user-ctx-4").copy(
            sessionHistory = listOf(
                PsySessionSummary("old-1", "Discussed work anxiety"),
                PsySessionSummary("old-2", "Discussed sleep"),
            ),
        )
        contextStore.saveProfile(profile)

        val ctx = contextStore.assembleContext(sessionId, "active")
        assertEquals(2, ctx.recentSessions.size)
    }

    @Test
    fun testAssembleContext_differentSessions_differentMessages() {
        val s1 = "s-ctx-5a"
        val s2 = "s-ctx-5b"
        contextStore.createSession(s1, "user-ctx-5")
        contextStore.createSession(s2, "user-ctx-5")

        contextStore.appendMessage(s1, ConversationEntry(MessageRole.USER, "A"))
        contextStore.appendMessage(s1, ConversationEntry(MessageRole.ASSISTANT, "B"))
        contextStore.appendMessage(s2, ConversationEntry(MessageRole.USER, "C"))
        contextStore.appendMessage(s2, ConversationEntry(MessageRole.ASSISTANT, "D"))

        val ctx1 = contextStore.assembleContext(s1, "active")
        val ctx2 = contextStore.assembleContext(s2, "active")

        assertEquals(2, ctx1.currentMessages.size)
        assertEquals(2, ctx2.currentMessages.size)
        assertEquals("A", ctx1.currentMessages[0].content)
        assertEquals("C", ctx2.currentMessages[0].content)
    }

    // ─── Memory debug API ─────────────────────────────────────────────────────

    @Test
    fun testMemoryDebug_sessionShowsMessageCount() = runTest {
        val sid = agent.startSession("user-debug-1")
        agent.chat(sid, "Hello there")
        val result = agent.chat(sid, "How are you?")
        // 2 user + 2 assistant = 4 messages
        assertEquals(4, result.session.messages.size)
    }

    @Test
    fun testMemoryDebug_profileShowsUpdatedName() = runTest {
        val sid = agent.startSession("user-debug-2")
        val result = agent.chat(sid, "Call me Katya")
        assertEquals("Katya", result.profile.preferredName)
    }

    // ─── Turn-level emotion and plan ─────────────────────────────────────────

    @Test
    fun testTurnContext_anxietyMessage_detectedEmotionSet() = runTest {
        val sid = agent.startSession("user-turn-1")
        val result = agent.chat(sid, "I feel really anxious lately")
        assertEquals("anxiety", result.turnContext.detectedEmotion)
    }

    @Test
    fun testTurnContext_noKeywords_detectedEmotionIsNull() = runTest {
        val sid = agent.startSession("user-turn-2")
        val result = agent.chat(sid, "Hello there")
        assertEquals(null, result.turnContext.detectedEmotion)
    }

    @Test
    fun testTurnContext_anyMessage_planIsNotNull() = runTest {
        val sid = agent.startSession("user-turn-3")
        val result = agent.chat(sid, "Hello there")
        assertNotNull(result.turnContext.plan)
    }

    @Test
    fun testTurnContext_emotionMessage_planIsValidateAndExplore() = runTest {
        val sid = agent.startSession("user-turn-4")
        val result = agent.chat(sid, "I feel sad and hopeless")
        assertEquals("validate_and_explore", result.turnContext.plan)
    }

    // ─── Session-level detected emotions ─────────────────────────────────────

    @Test
    fun testSession_anxietyMessage_detectedEmotionsAccumulated() = runTest {
        val sid = agent.startSession("user-session-emo-1")
        val result = agent.chat(sid, "I feel really anxious lately")
        assertTrue("anxiety" in result.session.detectedEmotions,
            "Expected 'anxiety' in session.detectedEmotions: ${result.session.detectedEmotions}")
    }

    @Test
    fun testSession_multipleTurns_emotionsAccumulate() = runTest {
        val sid = agent.startSession("user-session-emo-2")
        agent.chat(sid, "I feel anxious")
        val result = agent.chat(sid, "I cant sleep at night")
        assertTrue("anxiety" in result.session.detectedEmotions)
        assertTrue("sleep issues" in result.session.detectedEmotions)
    }

    @Test
    fun testSession_noKeywords_detectedEmotionsEmpty() = runTest {
        val sid = agent.startSession("user-session-emo-3")
        val result = agent.chat(sid, "Hello there")
        assertTrue(result.session.detectedEmotions.isEmpty(),
            "Expected empty detectedEmotions, got: ${result.session.detectedEmotions}")
    }
}
