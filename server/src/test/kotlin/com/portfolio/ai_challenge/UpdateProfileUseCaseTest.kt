package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.psy_agent.ProfileExtractor
import com.portfolio.ai_challenge.agent.psy_agent.UpdateProfileUseCase
import com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateProfileUseCaseTest {

    private val store = InMemoryContextStore()
    private val useCase = UpdateProfileUseCase(ProfileExtractor(), store)

    @Test
    fun testExecute_noExtractableInfo_profileUnchanged() {
        store.createSession("s1", "user1")
        useCase.execute("user1", "Hello there")

        val profile = store.loadProfile("user1")
        assertNull(profile.preferredName)
        assertEquals(emptyList(), profile.primaryConcerns)
        assertEquals(emptyList(), profile.knownTriggers)
    }

    @Test
    fun testExecute_messageWithName_setsPreferredName() {
        store.createSession("s2", "user2")
        useCase.execute("user2", "My name is Katya")

        assertEquals("Katya", store.loadProfile("user2").preferredName)
    }

    @Test
    fun testExecute_callMePattern_setsPreferredName() {
        store.createSession("s3", "user3")
        useCase.execute("user3", "Call me Kolian")

        assertEquals("Kolian", store.loadProfile("user3").preferredName)
    }

    @Test
    fun testExecute_messageWithAnxiety_addsConcern() {
        store.createSession("s4", "user4")
        useCase.execute("user4", "I feel really anxious lately")

        assertEquals(listOf("anxiety"), store.loadProfile("user4").primaryConcerns)
    }

    @Test
    fun testExecute_messageWithWorkStress_addsTrigger() {
        store.createSession("s5", "user5")
        useCase.execute("user5", "Work deadlines stress me out")

        assertEquals(listOf("work stress"), store.loadProfile("user5").knownTriggers)
    }

    @Test
    fun testExecute_repeatedConcern_noDuplicates() {
        store.createSession("s6", "user6")
        useCase.execute("user6", "I feel anxious")
        useCase.execute("user6", "I feel anxious again")

        assertEquals(listOf("anxiety"), store.loadProfile("user6").primaryConcerns)
    }

    @Test
    fun testExecute_existingNameNotOverwritten_whenMessageHasNoName() {
        store.createSession("s7", "user7")
        store.saveProfile(store.loadProfile("user7").copy(preferredName = "Alice"))

        useCase.execute("user7", "I cant sleep at night")

        assertEquals("Alice", store.loadProfile("user7").preferredName)
        assertEquals(listOf("sleep issues"), store.loadProfile("user7").primaryConcerns)
    }

    @Test
    fun testExecute_multipleConcernsAndTriggers_allAdded() {
        store.createSession("s8", "user8")
        useCase.execute("user8", "I feel so lonely and isolated, work deadlines are killing me")

        val profile = store.loadProfile("user8")
        assertEquals(listOf("work stress", "loneliness"), profile.knownTriggers)
    }

    @Test
    fun testExecute_accumulatesConcernsAcrossCalls() {
        store.createSession("s9", "user9")
        useCase.execute("user9", "I cant sleep at night")
        useCase.execute("user9", "I feel sad and hopeless")

        val profile = store.loadProfile("user9")
        assertEquals(listOf("sleep issues", "depression"), profile.primaryConcerns)
    }
}
