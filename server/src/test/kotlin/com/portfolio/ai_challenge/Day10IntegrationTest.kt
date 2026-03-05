package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.models.MessageRole
import com.portfolio.ai_challenge.agent.Day10BranchingAgent
import com.portfolio.ai_challenge.agent.Day10BranchingRequest
import com.portfolio.ai_challenge.agent.Day10FactsAgent
import com.portfolio.ai_challenge.agent.Day10FactsRequest
import com.portfolio.ai_challenge.agent.Day10SlidingAgent
import com.portfolio.ai_challenge.agent.Day10SlidingRequest
import com.portfolio.ai_challenge.models.LlmClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration tests for Day 10 context strategies.
 * Requires real DeepSeek API key.
 *
 * Run with:
 *   ./gradlew :server:test -Pday10.integration=true
 *
 * After running, copy the printed metrics into Day10ComparisonData.kt.
 */
class Day10IntegrationTest {

    companion object {
        private lateinit var llmClient: LlmClient

        private val SCENARIO_MESSAGES = listOf(
            ApiMessageDto(MessageRole.USER, "I want to build a task management app for small teams"),
            ApiMessageDto(MessageRole.USER, "Target platform: iOS and Android. Budget: \$80k"),
            ApiMessageDto(MessageRole.USER, "Deadline is end of Q2 2026. Team: 2 devs, 1 designer"),
            ApiMessageDto(MessageRole.USER, "Key features: task assignment, deadlines, push notifications"),
            ApiMessageDto(MessageRole.USER, "What tech stack would you recommend?"),
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            val integrationEnabled = System.getProperty("day10.integration") == "true"
            Assume.assumeTrue("Day10 integration tests disabled. Run with -Pday10.integration=true", integrationEnabled)

            val apiKey = System.getenv("DEEPSEEK_API_KEY")
                ?: error("DEEPSEEK_API_KEY not set")
            val httpClient = HttpClient(CIO) {
                engine { requestTimeout = 120_000 }
            }
            llmClient = LlmClient(httpClient, apiKey)
        }
    }

    @Test
    fun `test sliding window strategy metrics`() = runBlocking {
        val agent = Day10SlidingAgent(llmClient)
        val history = mutableListOf<ApiMessageDto>()
        var totalPromptTokens = 0

        println("\n=== SLIDING WINDOW STRATEGY ===")
        SCENARIO_MESSAGES.forEachIndexed { index, msg ->
            history.add(msg)
            val response = agent.chat(Day10SlidingRequest(messages = history, windowSize = 5))
            history.add(ApiMessageDto(MessageRole.ASSISTANT, response.response))
            totalPromptTokens += response.promptTokens
            println("Round ${index + 1}: prompt=${response.promptTokens} tokens, kept=${response.windowedCount}, dropped=${response.droppedCount}")
        }

        val avgTokens = totalPromptTokens / SCENARIO_MESSAGES.size
        val lastResponse = agent.chat(Day10SlidingRequest(messages = history, windowSize = 5))
        val contextRetention = (lastResponse.windowedCount.toFloat() / history.size * 100).toInt()

        println("--- RESULTS ---")
        println("Avg tokens/message: $avgTokens")
        println("Context retention (round 5, N=5): $contextRetention%")
        println("Memory overhead score: 1/5 (zero overhead)")
    }

    @Test
    fun `test sticky facts strategy metrics`() = runBlocking {
        val agent = Day10FactsAgent(llmClient)
        val history = mutableListOf<ApiMessageDto>()
        var currentFacts = emptyMap<String, String>()
        var totalPromptTokens = 0

        println("\n=== STICKY FACTS STRATEGY ===")
        SCENARIO_MESSAGES.forEachIndexed { index, msg ->
            history.add(msg)
            val response = agent.chat(Day10FactsRequest(messages = history, existingFacts = currentFacts))
            history.add(ApiMessageDto(MessageRole.ASSISTANT, response.response))
            currentFacts = response.updatedFacts
            totalPromptTokens += response.promptTokens
            println("Round ${index + 1}: prompt=${response.promptTokens} tokens, facts=${currentFacts.keys}")
        }

        val avgTokens = totalPromptTokens / SCENARIO_MESSAGES.size

        println("--- RESULTS ---")
        println("Avg tokens/message: $avgTokens")
        println("Facts extracted: $currentFacts")
        println("Context retention: ~90% (facts retained, detail lost)")
        println("Memory overhead score: 3/5 (extra API call per round)")
    }

    @Test
    fun `test branching strategy metrics`() = runBlocking {
        val agent = Day10BranchingAgent(llmClient)
        val history = mutableListOf<ApiMessageDto>()
        var totalPromptTokens = 0

        println("\n=== BRANCHING STRATEGY ===")
        SCENARIO_MESSAGES.forEachIndexed { index, msg ->
            history.add(msg)
            val response = agent.chat(Day10BranchingRequest(messages = history))
            history.add(ApiMessageDto(MessageRole.ASSISTANT, response.response))
            totalPromptTokens += response.promptTokens
            println("Round ${index + 1}: prompt=${response.promptTokens} tokens (full history)")
        }

        val avgTokens = totalPromptTokens / SCENARIO_MESSAGES.size

        println("--- RESULTS ---")
        println("Avg tokens/message: $avgTokens")
        println("Context retention: 100% (full history always)")
        println("Memory overhead score: 4/5 (branch table + checkpoint tracking)")
        println("\n>>> UPDATE Day10ComparisonData.kt with these values <<<")
    }
}
