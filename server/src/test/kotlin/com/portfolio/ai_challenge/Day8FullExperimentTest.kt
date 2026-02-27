package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.experiment.PrintlnExperimentLogger
import com.portfolio.ai_challenge.experiment.runFullExperiment
import com.portfolio.ai_challenge.experiment.saveResults
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Day8FullExperimentTest {

    private val apiKey = System.getenv("DEEPSEEK_API_KEY")

    private fun loadResource(name: String): String {
        val stream = checkNotNull(
            Day8FullExperimentTest::class.java.classLoader.getResourceAsStream("day-8-test-data/$name")
        ) { "Resource not found: day-8-test-data/$name" }
        return stream.bufferedReader().readText()
    }

    @Test
    fun `full experiment - all 3 cases`() {
        if (apiKey.isNullOrBlank()) {
            println("SKIPPED: DEEPSEEK_API_KEY not set")
            return
        }

        val outputDir = File(System.getProperty("user.dir"), "src/test/resources/day-8-results")
        outputDir.mkdirs()
        val outputPath = File(outputDir, "experiment-results.json").absolutePath

        val httpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 10_000
            }
        }

        val result = runBlocking {
            runFullExperiment(
                httpClient = httpClient,
                apiKey = apiKey,
                logger = PrintlnExperimentLogger(),
                outputPath = outputPath,
            ) { name -> loadResource(name) }
        }

        httpClient.close()

        assertTrue(result.cases.size == 3, "Must have 3 case results")
        result.cases.forEach { case ->
            println("${case.caseName}: peakTokens=${case.peakTokens}, steps=${case.totalSteps}, failed=${case.failedSteps}")
        }
        println("Results saved to: $outputPath")
    }
}
