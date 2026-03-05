package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.day_11_psy_agent.Day12PsyAgent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PersonalizeResponseUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.ProfileExtractor
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyChatResponse
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyPromptBuilder
import com.portfolio.ai_challenge.agent.day_11_psy_agent.PsyResponseMapper
import com.portfolio.ai_challenge.agent.day_11_psy_agent.UpdatePreferencesUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.UpdateProfileUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.InMemoryContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.CommunicationPreferences
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.Formality
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsySessionSummary
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ResponseLength
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.routes.day12PsyAgentRoutes
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Day12PersonalizationTest {

    private val personalizeUseCase = PersonalizeResponseUseCase()

    private fun storeWithProfile(
        userId: String = "u1",
        sessionId: String = "s1",
        preferences: CommunicationPreferences = CommunicationPreferences(),
        preferredName: String? = null,
        concerns: List<String> = emptyList(),
        triggers: List<String> = emptyList(),
    ): InMemoryContextStore {
        val store = InMemoryContextStore()
        store.createSession(sessionId, userId)
        store.saveProfile(store.loadProfile(userId).copy(
            preferredName = preferredName,
            primaryConcerns = concerns,
            knownTriggers = triggers,
            preferences = preferences,
        ))
        return store
    }

    private fun contextFor(store: InMemoryContextStore, sessionId: String = "s1") =
        store.assembleContext(sessionId, "active")

    // --- PersonalizeResponseUseCase: formality ---

    @Test
    fun testBuildPrompt_formalProfile_containsFormalInstruction() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.FORMAL))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("formal" in prompt.lowercase() || "respect" in prompt.lowercase(), "Expected formal instruction in prompt")
    }

    @Test
    fun testBuildPrompt_informalProfile_containsInformalInstruction() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.INFORMAL))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("warm" in prompt.lowercase() || "casual" in prompt.lowercase() || "friendly" in prompt.lowercase())
    }

    @Test
    fun testBuildPrompt_shortLength_containsShortInstruction() {
        val store = storeWithProfile(preferences = CommunicationPreferences(responseLength = ResponseLength.SHORT))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("concise" in prompt.lowercase() || "short" in prompt.lowercase() || "2-3" in prompt)
    }

    @Test
    fun testBuildPrompt_detailedLength_containsDetailedInstruction() {
        val store = storeWithProfile(preferences = CommunicationPreferences(responseLength = ResponseLength.DETAILED))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("detailed" in prompt.lowercase() || "thorough" in prompt.lowercase())
    }

    @Test
    fun testBuildPrompt_avoidTopics_listedInPrompt() {
        val store = storeWithProfile(preferences = CommunicationPreferences(avoidTopics = listOf("medication", "diagnosis")))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("medication" in prompt && "diagnosis" in prompt)
    }

    @Test
    fun testBuildPrompt_languageUk_promptContainsLanguage() {
        val store = storeWithProfile(preferences = CommunicationPreferences(language = "uk"))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("uk" in prompt)
    }

    @Test
    fun testBuildPrompt_withConcerns_promptContainsConcerns() {
        val store = storeWithProfile(concerns = listOf("anxiety", "sleep issues"))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("anxiety" in prompt && "sleep issues" in prompt)
    }

    @Test
    fun testBuildPrompt_withTriggers_promptContainsTriggers() {
        val store = storeWithProfile(triggers = listOf("work stress"))
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("work stress" in prompt)
    }

    @Test
    fun testBuildPrompt_newUser_promptSaysNewClient() {
        val store = storeWithProfile()
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("new client" in prompt.lowercase())
    }

    @Test
    fun testBuildPrompt_withPreferredName_promptContainsName() {
        val store = storeWithProfile(preferredName = "Alice")
        val prompt = personalizeUseCase.buildPersonalizedSystemPrompt(contextFor(store))
        assertTrue("Alice" in prompt)
    }

    // --- Session bridge ---

    @Test
    fun testSessionBridge_noHistory_returnsEmpty() {
        val result = personalizeUseCase.buildSessionBridge(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testSessionBridge_oneSession_containsTopics() {
        val sessions = listOf(
            PsySessionSummary("s1", "summary", topicsDiscussed = listOf("anxiety", "sleep"), techniquesUsed = listOf("breathing"), homework = "journal daily"),
        )
        val result = personalizeUseCase.buildSessionBridge(sessions)
        assertTrue("anxiety" in result && "sleep" in result && "breathing" in result)
    }

    @Test
    fun testSessionBridge_threeSessionsMax_onlyLastThree() {
        val sessions = (1..5).map { i ->
            PsySessionSummary("s$i", "summary $i", topicsDiscussed = listOf("topic$i"), techniquesUsed = listOf("tech$i"))
        }
        val result = personalizeUseCase.buildSessionBridge(sessions)
        assertFalse("topic1" in result, "Should not include session 1 (only last 3)")
        assertFalse("topic2" in result, "Should not include session 2 (only last 3)")
        assertTrue("topic3" in result && "topic4" in result && "topic5" in result)
    }

    // --- UpdatePreferencesUseCase ---

    @Test
    fun testUpdatePreferences_changesFormality() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.INFORMAL))
        val useCase = UpdatePreferencesUseCase(store)
        useCase.execute("u1", CommunicationPreferences(formality = Formality.FORMAL))
        assertEquals(Formality.FORMAL, store.loadProfile("u1").preferences.formality)
    }

    @Test
    fun testUpdatePreferences_doesNotOverwriteName() {
        val store = storeWithProfile(preferredName = "Bob")
        val useCase = UpdatePreferencesUseCase(store)
        useCase.execute("u1", CommunicationPreferences(formality = Formality.FORMAL))
        assertEquals("Bob", store.loadProfile("u1").preferredName)
    }

    @Test
    fun testUpdatePreferences_doesNotOverwriteConcerns() {
        val store = storeWithProfile(concerns = listOf("anxiety"))
        val useCase = UpdatePreferencesUseCase(store)
        useCase.execute("u1", CommunicationPreferences(formality = Formality.FORMAL))
        assertEquals(listOf("anxiety"), store.loadProfile("u1").primaryConcerns)
    }

    @Test
    fun testUpdatePreferences_avoidTopicsUpdated() {
        val store = storeWithProfile()
        val useCase = UpdatePreferencesUseCase(store)
        useCase.execute("u1", CommunicationPreferences(avoidTopics = listOf("medication")))
        assertEquals(listOf("medication"), store.loadProfile("u1").preferences.avoidTopics)
    }

    @Test
    fun testUpdatePreferences_persistsAcrossSessions() {
        val store = storeWithProfile()
        val useCase = UpdatePreferencesUseCase(store)
        useCase.execute("u1", CommunicationPreferences(language = "uk"))
        store.createSession("s2", "u1")
        val profileFromNewSession = store.loadProfile("u1")
        assertEquals("uk", profileFromNewSession.preferences.language)
    }

    // --- Two profiles comparison ---

    @Test
    fun testTwoProfiles_differentStyles_differentPrompts() {
        val storeA = storeWithProfile(userId = "a", sessionId = "sa", preferences = CommunicationPreferences(formality = Formality.FORMAL, responseLength = ResponseLength.SHORT))
        val storeB = storeWithProfile(userId = "b", sessionId = "sb", preferences = CommunicationPreferences(formality = Formality.INFORMAL, responseLength = ResponseLength.DETAILED))
        val promptA = personalizeUseCase.buildPersonalizedSystemPrompt(storeA.assembleContext("sa", "active"))
        val promptB = personalizeUseCase.buildPersonalizedSystemPrompt(storeB.assembleContext("sb", "active"))
        assertNotEquals(promptA, promptB)
    }

    // --- E2E agent tests with mock LLM ---

    private fun buildAgent(store: InMemoryContextStore, mockLlm: LlmClient): Day12PsyAgent {
        val personalizeUseCase = PersonalizeResponseUseCase()
        val promptBuilder = PsyPromptBuilder(ContextWindowManager(), personalizeUseCase)
        val updateProfile = UpdateProfileUseCase(ProfileExtractor(), store)
        return Day12PsyAgent(store, promptBuilder, mockLlm, updateProfile)
    }

    @Test
    fun testAgent_formalProfile_promptSentToLlmContainsFormal() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.FORMAL))
        val mockLlm = mockk<LlmClient>()
        val capturedMessages = slot<List<DeepSeekMessage>>()
        coEvery { mockLlm.complete(capture(capturedMessages), any(), any()) } returns "I understand."
        val agent = buildAgent(store, mockLlm)
        runBlocking { agent.chat("s1", "Hello") }
        val systemContent = capturedMessages.captured.filter { it.role.name == "SYSTEM" }.joinToString(" ") { it.content }
        assertTrue("formal" in systemContent.lowercase() || "respect" in systemContent.lowercase())
    }

    @Test
    fun testAgent_informalProfile_promptSentToLlmContainsInformal() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.INFORMAL))
        val mockLlm = mockk<LlmClient>()
        val capturedMessages = slot<List<DeepSeekMessage>>()
        coEvery { mockLlm.complete(capture(capturedMessages), any(), any()) } returns "Hey there!"
        val agent = buildAgent(store, mockLlm)
        runBlocking { agent.chat("s1", "Hello") }
        val systemContent = capturedMessages.captured.filter { it.role.name == "SYSTEM" }.joinToString(" ") { it.content }
        assertTrue("warm" in systemContent.lowercase() || "casual" in systemContent.lowercase() || "friendly" in systemContent.lowercase())
    }

    @Test
    fun testAgent_avoidTopics_promptSentToLlmContainsAvoidance() {
        val store = storeWithProfile(preferences = CommunicationPreferences(avoidTopics = listOf("medication")))
        val mockLlm = mockk<LlmClient>()
        val capturedMessages = slot<List<DeepSeekMessage>>()
        coEvery { mockLlm.complete(capture(capturedMessages), any(), any()) } returns "I hear you."
        val agent = buildAgent(store, mockLlm)
        runBlocking { agent.chat("s1", "Hello") }
        val systemContent = capturedMessages.captured.filter { it.role.name == "SYSTEM" }.joinToString(" ") { it.content }
        assertTrue("medication" in systemContent)
    }

    @Test
    fun testAgent_profileUpdatesReturned_inChatResult() {
        val store = storeWithProfile()
        val mockLlm = mockk<LlmClient>()
        coEvery { mockLlm.complete(any(), any(), any()) } returns "Nice to meet you!"
        val agent = buildAgent(store, mockLlm)
        val result = runBlocking { agent.chat("s1", "My name is Alice and I feel anxious") }
        assertTrue(result.profileUpdates.isNotEmpty())
        assertTrue(result.profileUpdates.any { "name" in it || "concern" in it })
    }

    @Test
    fun testAgent_preferenceChangeAffectsNextMessage() {
        val store = storeWithProfile(preferences = CommunicationPreferences(formality = Formality.INFORMAL))
        val mockLlm = mockk<LlmClient>()
        val messages1 = slot<List<DeepSeekMessage>>()
        val messages2 = slot<List<DeepSeekMessage>>()
        coEvery { mockLlm.complete(capture(messages1), any(), any()) } returns "Hey there!"
        val agent = buildAgent(store, mockLlm)
        runBlocking { agent.chat("s1", "Hello") }
        val informalContent = messages1.captured.filter { it.role.name == "SYSTEM" }.joinToString(" ") { it.content }

        store.saveProfile(store.loadProfile("u1").copy(preferences = CommunicationPreferences(formality = Formality.FORMAL)))
        coEvery { mockLlm.complete(capture(messages2), any(), any()) } returns "Good day."
        runBlocking { agent.chat("s1", "Hello again") }
        val formalContent = messages2.captured.filter { it.role.name == "SYSTEM" }.joinToString(" ") { it.content }

        assertNotEquals(informalContent, formalContent)
    }

    // --- Profile API route tests ---

    private fun buildTestApp(block: io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) = testApplication {
        val store = InMemoryContextStore()
        store.createSession("s1", "user1")
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { day12PsyAgentRoutes(mockk<Day12PsyAgent>(relaxed = true), PsyResponseMapper(), UpdatePreferencesUseCase(store), store) }
        block()
    }

    @Test
    fun testGetProfile_existingUser_returnsProfile() = testApplication {
        val store = InMemoryContextStore()
        store.createSession("s1", "user1")
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { day12PsyAgentRoutes(mockk<Day12PsyAgent>(relaxed = true), PsyResponseMapper(), UpdatePreferencesUseCase(store), store) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.get("/api/agent/psy12/profile?userId=user1")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGetProfile_unknownUser_returnsEmptyProfile() = testApplication {
        val store = InMemoryContextStore()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { day12PsyAgentRoutes(mockk<Day12PsyAgent>(relaxed = true), PsyResponseMapper(), UpdatePreferencesUseCase(store), store) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.get("/api/agent/psy12/profile?userId=unknown")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testUpdatePreferences_validRequest_returns200() = testApplication {
        val store = InMemoryContextStore()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { day12PsyAgentRoutes(mockk<Day12PsyAgent>(relaxed = true), PsyResponseMapper(), UpdatePreferencesUseCase(store), store) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/api/agent/psy12/profile/preferences") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "user1"); put("formality", "FORMAL"); put("responseLength", "SHORT") }.toString())
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testUpdatePreferences_thenGetProfile_reflectsChanges() = testApplication {
        val store = InMemoryContextStore()
        install(ServerContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        routing { day12PsyAgentRoutes(mockk<Day12PsyAgent>(relaxed = true), PsyResponseMapper(), UpdatePreferencesUseCase(store), store) }

        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        client.post("/api/agent/psy12/profile/preferences") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("userId", "user2"); put("formality", "FORMAL") }.toString())
        }
        val profileResponse = client.get("/api/agent/psy12/profile?userId=user2")
        assertEquals(HttpStatusCode.OK, profileResponse.status)
        assertNotNull(profileResponse.body<String>().also { assertTrue("FORMAL" in it) })
    }
}
