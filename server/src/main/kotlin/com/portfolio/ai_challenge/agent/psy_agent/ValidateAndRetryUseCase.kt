package com.portfolio.ai_challenge.agent.psy_agent

import io.github.oshai.kotlinlogging.KotlinLogging
import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantChecker
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantPromptInjector
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity
import com.github.michaelbull.result.getOrElse
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

/**
 * VALIDATE phase of the pipeline.
 *
 * Checks the LLM response against [InvariantChecker]. On [Severity.HARD_BLOCK]:
 * retries with [InvariantPromptInjector]-enriched prompt up to [MAX_RETRIES] times.
 * If all retries fail, returns [FALLBACK_RESPONSE].
 */
private val logger = KotlinLogging.logger {}

class ValidateAndRetryUseCase(
    private val checker: InvariantChecker,
    private val injector: InvariantPromptInjector,
    private val llmClient: LlmClient,
) {

    data class ValidationResult(
        val response: String,
        val attemptCount: Int,
        val violations: List<String>,
    )

    suspend fun execute(initialResponse: String, messages: List<DeepSeekMessage>): ValidationResult {
        var response = initialResponse
        var attemptCount = 1
        val violations = mutableListOf<String>()

        while (attemptCount <= MAX_RETRIES) {
            val hardBlock = checker.getFirstHardBlock(response)
            if (hardBlock == null) {
                val softViolations = checker.checkAll(response)
                    .filter { it.severity == Severity.SOFT_FIX }
                    .map { it.invariantName }
                return ValidationResult(response, attemptCount, violations + softViolations)
            }
            violations.add(hardBlock.invariantName)
            logger.info { "Retry $attemptCount/$MAX_RETRIES: hard block on ${hardBlock.invariantName}" }
            if (attemptCount >= MAX_RETRIES) break
            response = llmClient.complete(withInjectedConstraints(messages), maxTokens = 300)
                .getOrElse {
                    logger.warn { "LLM error during retry $attemptCount: $it" }
                    return ValidationResult(FALLBACK_RESPONSE, attemptCount, violations + "llm_error")
                }
            attemptCount++
        }
        return ValidationResult(FALLBACK_RESPONSE, attemptCount, violations)
    }

    private fun withInjectedConstraints(messages: List<DeepSeekMessage>): List<DeepSeekMessage> {
        val first = messages.indexOfFirst { it.role == MessageRole.SYSTEM }
        if (first == -1) return messages
        return messages.toMutableList().also {
            it[first] = it[first].copy(content = injector.inject(it[first].content))
        }
    }

    companion object {
        const val MAX_RETRIES = 3
        val FALLBACK_RESPONSE by lazy { Prompts.Psy.FALLBACK }
    }
}
