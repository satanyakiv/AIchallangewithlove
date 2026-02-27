package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.experiment.models.TestStep
import com.portfolio.ai_challenge.experiment.models.content
import com.portfolio.ai_challenge.experiment.models.type
import com.portfolio.ai_challenge.experiment.parseTestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestDataParserTest {

    private fun loadResource(name: String): String {
        val stream = checkNotNull(
            TestDataParserTest::class.java.classLoader.getResourceAsStream("day-8-test-data/$name")
        ) { "Resource not found: day-8-test-data/$name" }
        return stream.bufferedReader().readText()
    }

    @Test
    fun `case_1_short has non-empty system prompt`() {
        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        assertTrue(testCase.systemPrompt.isNotBlank(), "System prompt must not be blank")
        assertTrue(testCase.systemPrompt.contains("NOTABLE"), "System prompt should contain the NOTABLE rule")
    }

    @Test
    fun `case_1_short has 3 messages and 4 verifications`() {
        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        val messages = testCase.steps.filterIsInstance<TestStep.Message>()
        val verifications = testCase.steps.filterIsInstance<TestStep.Verification>()
        val checkpoints = testCase.steps.filterIsInstance<TestStep.Checkpoint>()

        assertEquals(3, messages.size, "case_1_short must have 3 messages")
        assertEquals(4, verifications.size, "case_1_short must have 4 verifications")
        assertEquals(0, checkpoints.size, "case_1_short must have 0 checkpoints")
    }

    @Test
    fun `first message has correct id and non-empty content`() {
        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        val firstMessage = testCase.steps.filterIsInstance<TestStep.Message>().first()
        assertEquals("M1", firstMessage.id)
        assertTrue(firstMessage.content.isNotBlank())
    }

    @Test
    fun `verifications have correct ids and failure modes`() {
        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        val verifications = testCase.steps.filterIsInstance<TestStep.Verification>()
        assertEquals("V1-1", verifications[0].id)
        assertEquals("V1-2", verifications[1].id)
        assertEquals("V1-3", verifications[2].id)
        assertEquals("V1-4", verifications[3].id)
        assertTrue(verifications[0].failureMode.isNotBlank())
    }

    @Test
    fun `step types are correct strings`() {
        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        testCase.steps.filterIsInstance<TestStep.Message>().forEach {
            assertEquals("message", it.type)
        }
        testCase.steps.filterIsInstance<TestStep.Verification>().forEach {
            assertEquals("verification", it.type)
        }
    }

    @Test
    fun `note for autotest sections are skipped`() {
        val raw = loadResource("case_3_overflow.txt")
        val testCase = parseTestData("case_3_overflow", raw)

        val allContent = testCase.steps.joinToString { it.content }
        assertTrue(
            !allContent.contains("NOTE FOR AUTOTEST"),
            "NOTE FOR AUTOTEST sections must be excluded from steps"
        )
    }

    @Test
    fun `case_2_long has checkpoints`() {
        val raw = loadResource("case_2_long.txt")
        val testCase = parseTestData("case_2_long", raw)

        val checkpoints = testCase.steps.filterIsInstance<TestStep.Checkpoint>()
        assertTrue(checkpoints.isNotEmpty(), "case_2_long should have checkpoints")
        assertTrue(checkpoints[0].id.startsWith("C2-"))
    }
}
