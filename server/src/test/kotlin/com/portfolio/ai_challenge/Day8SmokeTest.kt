package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.experiment.PrintlnExperimentLogger
import com.portfolio.ai_challenge.experiment.models.TestStep
import com.portfolio.ai_challenge.experiment.models.type
import com.portfolio.ai_challenge.experiment.parseTestData
import com.portfolio.ai_challenge.experiment.saveResults
import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.experiment.Day8ExperimentRunner
import com.portfolio.ai_challenge.experiment.models.FullExperimentResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Day8SmokeTest {

    private val apiKey = System.getenv("DEEPSEEK_API_KEY")

    private fun loadResource(name: String): String {
        val stream = checkNotNull(
            Day8SmokeTest::class.java.classLoader.getResourceAsStream("day-8-test-data/$name")
        ) { "Resource not found: day-8-test-data/$name" }
        return stream.bufferedReader().readText()
    }

    @Test
    fun `case 1 smoke - parse and run all steps with real API`() {
        if (apiKey.isNullOrBlank()) {
            println("SKIPPED: DEEPSEEK_API_KEY not set")
            return
        }

        val raw = loadResource("case_1_short.txt")
        val testCase = parseTestData("case_1_short", raw)

        val httpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
            }
        }

        val logger = PrintlnExperimentLogger()
        val agent = Day8Agent(httpClient, apiKey, testCase.systemPrompt)
        val runner = Day8ExperimentRunner(agent, logger)

        val result = runBlocking { runner.runExperiment(testCase) }

        httpClient.close()

        // Basic assertions
        assertFalse(result.steps.isEmpty(), "Must have at least one step result")
        assertTrue(result.peakTokens > 0, "Peak tokens must be > 0")
        assertTrue(result.conversationLog.isNotEmpty(), "Conversation log must not be empty")

        val userEntries = result.conversationLog.filter { it.role == "user" }
        val assistantEntries = result.conversationLog.filter { it.role == "assistant" }
        assertTrue(userEntries.isNotEmpty(), "Must have user entries in log")
        assertTrue(assistantEntries.isNotEmpty(), "Must have assistant entries in log")

        val failedSteps = result.steps.filter { it.errorMessage != null }
        assertTrue(failedSteps.isEmpty(), "No steps should fail: ${failedSteps.map { it.stepId }}")

        // Save results
        val outputDir = File(System.getProperty("user.dir"), "src/test/resources/day-8-results")
        outputDir.mkdirs()
        val fullResult = FullExperimentResult(cases = listOf(result), generatedAt = System.currentTimeMillis())
        saveResults(fullResult, File(outputDir, "case_1_smoke_result.json").absolutePath)

        println("Peak tokens: ${result.peakTokens}")
        println("Total messages: ${result.totalMessages}")
        println("Steps: ${result.totalSteps}, failed: ${result.failedSteps}")
        println("Results saved to: ${outputDir.absolutePath}/case_1_smoke_result.json")
    }
}
