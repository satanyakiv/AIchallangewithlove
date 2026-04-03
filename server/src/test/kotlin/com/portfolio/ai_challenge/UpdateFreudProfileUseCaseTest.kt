package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.freud_agent.FreudProfileExtractor
import com.portfolio.ai_challenge.agent.freud_agent.UpdateFreudProfileUseCase
import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudInMemoryContextStore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateFreudProfileUseCaseTest {

    private val store = FreudInMemoryContextStore()
    private val useCase = UpdateFreudProfileUseCase(FreudProfileExtractor(), store)

    private fun setupUser(userId: String = "test-user") {
        val sessionId = "session-1"
        store.createSession(sessionId, userId)
    }

    @Test
    fun testExecute_nameExtracted_profileHasNewName() {
        setupUser()
        useCase.execute("test-user", "My name is Heinrich and I feel anxious")
        val profile = store.loadProfile("test-user")
        assertEquals("Heinrich", profile.patientName)
    }

    @Test
    fun testExecute_noNameInMessage_nameUnchanged() {
        setupUser()
        useCase.execute("test-user", "I feel a bit worried today")
        val profile = store.loadProfile("test-user")
        assertNull(profile.patientName)
    }

    @Test
    fun testExecute_dreamSymbol_persistsToProfile() {
        setupUser()
        useCase.execute("test-user", "I had a strange dream about flying last night")
        val profile = store.loadProfile("test-user")
        assertTrue(profile.dreamSymbols.isNotEmpty())
    }

    @Test
    fun testExecute_defenseMechanism_persistsToProfile() {
        setupUser()
        useCase.execute("test-user", "I deny that any of this matters")
        val profile = store.loadProfile("test-user")
        assertTrue(profile.defenseMechanisms.contains("denial"))
    }

    @Test
    fun testExecute_fixationDetected_persistsToProfile() {
        setupUser()
        useCase.execute("test-user", "I cannot stop eating junk food when stressed")
        val profile = store.loadProfile("test-user")
        assertEquals("oral", profile.fixationStage)
    }

    @Test
    fun testExecute_multipleMessages_accumulateMarkers() {
        setupUser()
        useCase.execute("test-user", "My mother always criticized me")
        useCase.execute("test-user", "I had a nightmare about falling")
        val profile = store.loadProfile("test-user")
        assertTrue(profile.childhoodThemes.contains("mother"))
        assertTrue(profile.dreamSymbols.isNotEmpty())
    }

    @Test
    fun testExecute_normalMessage_noProfileChanges() {
        setupUser()
        useCase.execute("test-user", "The weather is nice today")
        val profile = store.loadProfile("test-user")
        assertNull(profile.patientName)
        assertTrue(profile.defenseMechanisms.isEmpty())
        assertTrue(profile.childhoodThemes.isEmpty())
        assertTrue(profile.dreamSymbols.isEmpty())
        assertNull(profile.fixationStage)
        assertTrue(profile.relationshipPatterns.isEmpty())
    }

    // --- Language persistence tests ---

    @Test
    fun testExecute_ukrainianMessage_languagePersisted() {
        setupUser()
        useCase.execute("test-user", "Мені сниться один і той самий сон")
        val profile = store.loadProfile("test-user")
        assertEquals("uk", profile.language)
    }

    @Test
    fun testExecute_englishMessage_languageStaysDefault() {
        setupUser()
        useCase.execute("test-user", "I feel anxious today")
        val profile = store.loadProfile("test-user")
        assertEquals("en", profile.language)
    }

    @Test
    fun testExecute_languageSwitchMidConversation_updatesToNewLanguage() {
        setupUser()
        useCase.execute("test-user", "Hello, my name is Anna")
        assertEquals("en", store.loadProfile("test-user").language)
        useCase.execute("test-user", "Можемо говорити українською?")
        assertEquals("uk", store.loadProfile("test-user").language)
    }
}
