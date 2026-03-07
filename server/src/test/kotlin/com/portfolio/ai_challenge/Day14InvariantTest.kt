package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantChecker
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantPromptInjector
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantResult
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoDiagnosisInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoMedicationInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoPromptLeakInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.NoProfanityInvariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.impl.ResponseLengthInvariant
import com.portfolio.ai_challenge.agent.psy_agent.ValidateAndRetryUseCase
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Day14InvariantTest {

    // ── NoDiagnosisInvariant ──────────────────────────────────────────────────

    @Test
    fun testNoDiagnosis_diagnosisResponse_returnsViolated() {
        val result = NoDiagnosisInvariant().check("You have depression and anxiety.")
        assertTrue(result is InvariantResult.Violated)
        assertEquals(Severity.HARD_BLOCK, (result as InvariantResult.Violated).severity)
    }

    @Test
    fun testNoDiagnosis_cleanResponse_returnsPassed() {
        val result = NoDiagnosisInvariant().check("What you're describing sounds very challenging.")
        assertEquals(InvariantResult.Passed, result)
    }

    // ── NoMedicationInvariant ─────────────────────────────────────────────────

    @Test
    fun testNoMedication_medicationSuggestion_returnsViolated() {
        val result = NoMedicationInvariant().check("You should take Prozac for that.")
        assertTrue(result is InvariantResult.Violated)
        assertEquals(Severity.HARD_BLOCK, (result as InvariantResult.Violated).severity)
    }

    @Test
    fun testNoMedication_cleanResponse_returnsPassed() {
        val result = NoMedicationInvariant().check("A psychiatrist could discuss options with you.")
        assertEquals(InvariantResult.Passed, result)
    }

    // ── NoProfanityInvariant ──────────────────────────────────────────────────

    @Test
    fun testNoProfanity_profanityInResponse_returnsViolated_softFix() {
        val result = NoProfanityInvariant().check("That's some bullshit advice from others.")
        // "bullshit" contains "shit" — should violate
        // or use a direct word from the list
        val result2 = NoProfanityInvariant().check("What the fuck is going on?")
        assertTrue(result2 is InvariantResult.Violated)
        assertEquals(Severity.SOFT_FIX, (result2 as InvariantResult.Violated).severity)
    }

    @Test
    fun testNoProfanity_cleanResponse_returnsPassed() {
        val result = NoProfanityInvariant().check("I hear you. That sounds really difficult.")
        assertEquals(InvariantResult.Passed, result)
    }

    // ── ResponseLengthInvariant ───────────────────────────────────────────────

    @Test
    fun testResponseLength_tooShort_returnsSoftFix() {
        val result = ResponseLengthInvariant().check("OK.")
        assertTrue(result is InvariantResult.Violated)
        assertEquals(Severity.SOFT_FIX, (result as InvariantResult.Violated).severity)
    }

    @Test
    fun testResponseLength_tooLong_returnsSoftFix() {
        val result = ResponseLengthInvariant().check("x".repeat(2001))
        assertTrue(result is InvariantResult.Violated)
        assertEquals(Severity.SOFT_FIX, (result as InvariantResult.Violated).severity)
    }

    @Test
    fun testResponseLength_validLength_returnsPassed() {
        val result = ResponseLengthInvariant().check("I understand how you feel. Let's explore this together.")
        assertEquals(InvariantResult.Passed, result)
    }

    // ── NoPromptLeakInvariant ─────────────────────────────────────────────────

    @Test
    fun testNoPromptLeak_aiDisclosure_returnsViolated() {
        val result = NoPromptLeakInvariant().check("As an AI, I cannot feel emotions.")
        assertTrue(result is InvariantResult.Violated)
        assertEquals(Severity.HARD_BLOCK, (result as InvariantResult.Violated).severity)
    }

    @Test
    fun testNoPromptLeak_cleanResponse_returnsPassed() {
        val result = NoPromptLeakInvariant().check("I'm here to support you through this.")
        assertEquals(InvariantResult.Passed, result)
    }

    // ── InvariantChecker ──────────────────────────────────────────────────────

    private val checker = InvariantChecker(
        listOf(NoDiagnosisInvariant(), NoMedicationInvariant(), NoProfanityInvariant(), ResponseLengthInvariant(), NoPromptLeakInvariant())
    )

    @Test
    fun testCheckAll_cleanResponse_returnsEmpty() {
        val violations = checker.checkAll("I hear what you're saying. That sounds very hard.")
        assertTrue(violations.isEmpty())
    }

    @Test
    fun testCheckAll_multipleViolations_returnsAll() {
        val violations = checker.checkAll("You have depression. I am an AI and I cannot feel.")
        assertTrue(violations.size >= 2)
    }

    @Test
    fun testHasHardBlock_hardBlockPresent_returnsTrue() {
        assertTrue(checker.hasHardBlock("You have depression."))
    }

    @Test
    fun testHasHardBlock_onlySoftFix_returnsFalse() {
        assertFalse(checker.hasHardBlock("OK.")) // too short = SOFT_FIX only
    }

    @Test
    fun testGetFirstHardBlock_hardBlockPresent_returnsFirst() {
        val block = checker.getFirstHardBlock("You have depression.")
        assertNotNull(block)
        assertEquals(Severity.HARD_BLOCK, block!!.severity)
    }

    @Test
    fun testGetFirstHardBlock_noHardBlock_returnsNull() {
        val block = checker.getFirstHardBlock("I hear you.")
        assertNull(block)
    }

    // ── InvariantPromptInjector ───────────────────────────────────────────────

    @Test
    fun testInject_appendsAllInstructions() {
        val invariants = listOf(NoDiagnosisInvariant(), NoMedicationInvariant())
        val injector = InvariantPromptInjector(invariants)
        val result = injector.inject("Base system prompt.")
        assertTrue(result.contains("Base system prompt."))
        assertTrue(result.contains(NoDiagnosisInvariant().toPromptInstruction()))
        assertTrue(result.contains(NoMedicationInvariant().toPromptInstruction()))
    }

    // ── ValidateAndRetryUseCase ───────────────────────────────────────────────

    private val messages = listOf(
        DeepSeekMessage(role = MessageRole.SYSTEM, content = "You are MindGuard."),
        DeepSeekMessage(role = MessageRole.USER, content = "I feel sad."),
    )

    @Test
    fun testValidate_cleanFirstResponse_returnsOnFirstAttempt() = runTest {
        val llm = mockk<LlmClient>()
        val useCase = ValidateAndRetryUseCase(checker, InvariantPromptInjector(listOf(NoDiagnosisInvariant())), llm)

        val result = useCase.execute("I hear you. That sounds really difficult.", messages)

        assertEquals("I hear you. That sounds really difficult.", result.response)
        assertEquals(1, result.attemptCount)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun testValidate_hardBlock_retriesAndReturnsCleaned() = runTest {
        val llm = mockk<LlmClient>()
        val cleanResponse = "What you're experiencing sounds very challenging."
        coEvery { llm.complete(any(), any(), any()) } returns cleanResponse

        val useCase = ValidateAndRetryUseCase(checker, InvariantPromptInjector(listOf(NoDiagnosisInvariant())), llm)

        val result = useCase.execute("You have depression.", messages)

        assertEquals(cleanResponse, result.response)
        assertEquals(2, result.attemptCount)
        assertTrue(result.violations.contains("NoDiagnosis"))
    }

    @Test
    fun testValidate_hardBlockAllAttempts_returnsFallback() = runTest {
        val llm = mockk<LlmClient>()
        coEvery { llm.complete(any(), any(), any()) } returns "You have depression."

        val useCase = ValidateAndRetryUseCase(checker, InvariantPromptInjector(listOf(NoDiagnosisInvariant())), llm)

        val result = useCase.execute("You have depression.", messages)

        assertEquals(ValidateAndRetryUseCase.FALLBACK_RESPONSE, result.response)
        assertEquals(ValidateAndRetryUseCase.MAX_RETRIES, result.attemptCount)
    }
}
