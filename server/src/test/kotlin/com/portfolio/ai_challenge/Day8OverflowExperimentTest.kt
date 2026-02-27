package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.experiment.Day8ExperimentRunner
import com.portfolio.ai_challenge.experiment.PrintlnExperimentLogger
import com.portfolio.ai_challenge.experiment.models.FullExperimentResult
import com.portfolio.ai_challenge.experiment.parseTestData
import com.portfolio.ai_challenge.experiment.saveResults
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Day8OverflowExperimentTest {

    private val apiKey = System.getenv("DEEPSEEK_API_KEY")

    private fun loadResource(name: String): String {
        val stream = checkNotNull(
            Day8OverflowExperimentTest::class.java.classLoader.getResourceAsStream("day-8-test-data/$name")
        ) { "Resource not found: day-8-test-data/$name" }
        return stream.bufferedReader().readText()
    }

    @Test
    fun `overflow experiment - case 3 as continuation of case 2`() {
        if (apiKey.isNullOrBlank()) {
            println("SKIPPED: DEEPSEEK_API_KEY not set")
            return
        }

        val outputDir = File(System.getProperty("user.dir"), "src/test/resources/day-8-results")
        outputDir.mkdirs()
        val outputPath = File(outputDir, "experiment-results-fixed.json").absolutePath

        val case2 = parseTestData("case_2_long", loadResource("case_2_long.txt"))
        val case3 = parseTestData("case_3_overflow", loadResource("case_3_overflow.txt"))

        val httpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000
                connectTimeoutMillis = 10_000
            }
        }

        val result = runBlocking {
            val agent = Day8Agent(
                httpClient = httpClient,
                apiKey = apiKey,
                systemPrompt = case2.systemPrompt,
            )
            val runner = Day8ExperimentRunner(agent = agent, logger = PrintlnExperimentLogger())
            runner.runOverflowExperiment(case2 = case2, case3 = case3)
        }

        httpClient.close()

        val fullResult = FullExperimentResult(cases = listOf(result), generatedAt = System.currentTimeMillis())
        saveResults(fullResult, outputPath)

        println("case_3_overflow_fixed: peakTokens=${result.peakTokens}, steps=${result.totalSteps}, failed=${result.failedSteps}")
        println("Results saved to: $outputPath")

        assertTrue(result.totalSteps > 0, "Must have steps")
        assertTrue(result.peakTokens > 0, "Must have token data")
    }
}
