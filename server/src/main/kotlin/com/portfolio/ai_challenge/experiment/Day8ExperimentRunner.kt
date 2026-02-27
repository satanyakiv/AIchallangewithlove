package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.agent.AgentResponse
import com.portfolio.ai_challenge.agent.ApiMessageDto
import com.portfolio.ai_challenge.agent.Day8Agent
import com.portfolio.ai_challenge.experiment.models.ConversationEntry
import com.portfolio.ai_challenge.experiment.models.ExperimentResult
import com.portfolio.ai_challenge.experiment.models.FullExperimentResult
import com.portfolio.ai_challenge.experiment.models.StepResult
import com.portfolio.ai_challenge.experiment.models.TestCase
import com.portfolio.ai_challenge.experiment.models.TestStep
import com.portfolio.ai_challenge.experiment.models.content
import com.portfolio.ai_challenge.experiment.models.failureMode
import com.portfolio.ai_challenge.experiment.models.id
import com.portfolio.ai_challenge.experiment.models.type
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val outputJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class Day8ExperimentRunner(
    private val agent: Day8Agent,
    private val logger: ExperimentLogger,
) {
    suspend fun runExperiment(testCase: TestCase): ExperimentResult {
        val startedAt = System.currentTimeMillis()
        val conversationHistory = mutableListOf<ApiMessageDto>()
        val stepResults = mutableListOf<StepResult>()
        val conversationLog = mutableListOf<ConversationEntry>()
        var peakTokens = 0

        suspend fun sendStep(userMessage: String, stepId: String, stepType: String, failureMode: String?): AgentResponse {
            conversationHistory.add(ApiMessageDto(role = "user", content = userMessage))
            conversationLog.add(
                ConversationEntry(
                    role = "user",
                    content = userMessage,
                    stepId = stepId,
                    stepType = stepType,
                    usage = null,
                    timestamp = System.currentTimeMillis(),
                )
            )

            val response = agent.chat(conversationHistory.toList())

            if (response.errorMessage == null) {
                conversationHistory.add(ApiMessageDto(role = "assistant", content = response.content))
                conversationLog.add(
                    ConversationEntry(
                        role = "assistant",
                        content = response.content,
                        stepId = stepId,
                        stepType = stepType,
                        usage = response.usage,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }

            response.usage?.totalTokens?.let { if (it > peakTokens) peakTokens = it }
            stepResults.add(
                StepResult(
                    stepId = stepId,
                    stepType = stepType,
                    failureMode = failureMode,
                    userMessage = userMessage,
                    assistantResponse = response.content.takeIf { it.isNotEmpty() },
                    usage = response.usage,
                    httpStatus = response.httpStatus,
                    errorMessage = response.errorMessage,
                    conversationLength = conversationHistory.size,
                    timestamp = System.currentTimeMillis(),
                )
            )
            return response
        }

        var aborted = false

        for (step in testCase.steps) {
            if (aborted) break

            when (step) {
                is TestStep.Checkpoint -> logger.logMilestone("CHECKPOINT ${step.id}: ${step.failureMode}")
                is TestStep.Verification -> logger.logMilestone("VERIFICATION ${step.id}: ${step.failureMode}")
                else -> {}
            }
            logger.logStep(step, conversationHistory.size)

            val response = sendStep(
                userMessage = step.content,
                stepId = step.id,
                stepType = step.type,
                failureMode = step.failureMode,
            )
            logger.logResponse(response)

            if (response.errorMessage != null) {
                logger.logError("Aborting experiment: ${response.errorMessage}")
                aborted = true
                break
            }

            delay(1000)
        }

        // Overflow loop: if peak tokens still low, repeat messages up to 3 cycles
        if (!aborted && peakTokens < 120_000) {
            val messages = testCase.steps.filterIsInstance<TestStep.Message>()
            for (cycle in 1..3) {
                if (aborted || peakTokens >= 120_000) break
                logger.logMilestone("Overflow repeat cycle $cycle/3 (peakTokens=$peakTokens)")

                for (msg in messages) {
                    if (aborted || peakTokens >= 120_000) break
                    val repeatId = "${msg.id}-R$cycle"
                    val repeatContent = "REPEAT: ${msg.content}"
                    logger.logStep(TestStep.Message(id = repeatId, content = repeatContent), conversationHistory.size)

                    val response = sendStep(
                        userMessage = repeatContent,
                        stepId = repeatId,
                        stepType = "message",
                        failureMode = null,
                    )
                    logger.logResponse(response)

                    if (response.errorMessage != null) {
                        logger.logError("Aborting overflow: ${response.errorMessage}")
                        aborted = true
                        break
                    }
                    delay(1000)
                }
            }
        }

        return ExperimentResult(
            caseName = testCase.name,
            steps = stepResults.toList(),
            conversationLog = conversationLog.toList(),
            totalMessages = conversationHistory.size,
            peakTokens = peakTokens,
            totalSteps = stepResults.size,
            failedSteps = stepResults.count { it.errorMessage != null },
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Runs Case 3 as a CONTINUATION of Case 2 to properly test long-context behavior:
     *
     * Phase 1 — Case 2 messages + checkpoints (no verifications)
     * Phase 2 — Case 3 messages + checkpoints (no verifications)
     * Phase 3 — Repetition rounds until 120K tokens (max 5 rounds)
     * Phase 4 — All verifications from BOTH cases at peak context
     */
    suspend fun runOverflowExperiment(
        case2: TestCase,
        case3: TestCase,
    ): ExperimentResult {
        val startedAt = System.currentTimeMillis()
        val conversationHistory = mutableListOf<ApiMessageDto>()
        val stepResults = mutableListOf<StepResult>()
        val conversationLog = mutableListOf<ConversationEntry>()
        var peakTokens = 0
        var lastTotalTokens = 0

        suspend fun sendStep(userMessage: String, stepId: String, stepType: String, failureMode: String?): AgentResponse {
            conversationHistory.add(ApiMessageDto(role = "user", content = userMessage))
            conversationLog.add(
                ConversationEntry(
                    role = "user",
                    content = userMessage,
                    stepId = stepId,
                    stepType = stepType,
                    usage = null,
                    timestamp = System.currentTimeMillis(),
                )
            )

            val response = agent.chat(conversationHistory.toList())

            if (response.errorMessage == null) {
                conversationHistory.add(ApiMessageDto(role = "assistant", content = response.content))
                conversationLog.add(
                    ConversationEntry(
                        role = "assistant",
                        content = response.content,
                        stepId = stepId,
                        stepType = stepType,
                        usage = response.usage,
                        timestamp = System.currentTimeMillis(),
                    )
                )
            }

            response.usage?.totalTokens?.let {
                if (it > peakTokens) peakTokens = it
                lastTotalTokens = it
            }
            stepResults.add(
                StepResult(
                    stepId = stepId,
                    stepType = stepType,
                    failureMode = failureMode,
                    userMessage = userMessage,
                    assistantResponse = response.content.takeIf { it.isNotEmpty() },
                    usage = response.usage,
                    httpStatus = response.httpStatus,
                    errorMessage = response.errorMessage,
                    conversationLength = conversationHistory.size,
                    timestamp = System.currentTimeMillis(),
                )
            )
            return response
        }

        // Phase 1: Case 2 messages + checkpoints (skip verifications)
        logger.logMilestone("Phase 1: Loading Case 2 — Studies 1-10 (messages + checkpoints)")
        var phaseAborted = false
        for (step in case2.steps.filter { it !is TestStep.Verification }) {
            if (phaseAborted) break
            if (step is TestStep.Checkpoint) logger.logMilestone("CHECKPOINT ${step.id}: ${step.failureMode}")
            logger.logStep(step, conversationHistory.size)
            val response = sendStep(step.content, step.id, step.type, step.failureMode)
            logger.logResponse(response)
            if (response.errorMessage != null) {
                logger.logError("Error in Phase 1 at ${step.id}: ${response.errorMessage}")
                phaseAborted = true
            }
            delay(1000)
        }
        logger.logMilestone("Phase 1 complete — Studies 1-10 loaded. Tokens: $lastTotalTokens")

        // Phase 2: Case 3 messages + checkpoints (skip verifications)
        if (!phaseAborted) {
            logger.logMilestone("Phase 2: Loading Case 3 — Studies 11-25 (messages + checkpoints)")
            for (step in case3.steps.filter { it !is TestStep.Verification }) {
                if (phaseAborted) break
                if (step is TestStep.Checkpoint) logger.logMilestone("CHECKPOINT ${step.id}: ${step.failureMode}")
                logger.logStep(step, conversationHistory.size)
                val response = sendStep(step.content, step.id, step.type, step.failureMode)
                logger.logResponse(response)
                if (response.errorMessage != null) {
                    logger.logError("Error in Phase 2 at ${step.id}: ${response.errorMessage}")
                    phaseAborted = true
                }
                delay(1000)
            }
            logger.logMilestone("Phase 2 complete — All 25 studies loaded. Tokens: $lastTotalTokens")
        }

        // Phase 3: Repetition rounds to push toward 128K
        val allMessages = (case2.steps + case3.steps).filterIsInstance<TestStep.Message>()
        var repetitionRound = 0
        while (lastTotalTokens < 120_000 && repetitionRound < 10) {
            repetitionRound++
            logger.logMilestone("Phase 3 — Repetition round R$repetitionRound. Current tokens: $lastTotalTokens")
            var errorInRound = false
            for (step in allMessages) {
                if (lastTotalTokens >= 125_000) break
                val repeatId = "${step.id}-R$repetitionRound"
                val repeatContent = "REPEAT R$repetitionRound: ${step.content}"
                logger.logStep(TestStep.Message(id = repeatId, content = repeatContent), conversationHistory.size)
                val response = sendStep(
                    userMessage = repeatContent,
                    stepId = repeatId,
                    stepType = "message",
                    failureMode = null,
                )
                logger.logResponse(response)
                if (response.errorMessage != null) {
                    logger.logError("Error in R$repetitionRound at $repeatId (continuing to Phase 4): ${response.errorMessage}")
                    errorInRound = true
                    break
                }
                delay(1000)
            }
            if (errorInRound) break
        }
        logger.logMilestone("Phase 3 complete — Repetitions done. Peak tokens: $peakTokens")

        // Phase 4: Send all verifications at peak context
        val case2Verifications = case2.steps.filterIsInstance<TestStep.Verification>()
        val case3Verifications = case3.steps.filterIsInstance<TestStep.Verification>()
        logger.logMilestone("Phase 4: Sending ${case2Verifications.size + case3Verifications.size} verifications at $lastTotalTokens tokens")
        for (step in case2Verifications + case3Verifications) {
            logger.logMilestone("VERIFICATION ${step.id}: ${step.failureMode}")
            logger.logStep(step, conversationHistory.size)
            val response = sendStep(step.content, step.id, step.type, step.failureMode)
            logger.logResponse(response)
            if (response.errorMessage != null) {
                logger.logError("Error in verification ${step.id}: ${response.errorMessage}")
            }
            delay(1000)
        }
        logger.logMilestone("Phase 4 complete — All verifications sent. Final peak tokens: $peakTokens")

        return ExperimentResult(
            caseName = "case_3_overflow_fixed",
            steps = stepResults.toList(),
            conversationLog = conversationLog.toList(),
            totalMessages = conversationHistory.size,
            peakTokens = peakTokens,
            totalSteps = stepResults.size,
            failedSteps = stepResults.count { it.errorMessage != null },
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
        )
    }
}

suspend fun runFullExperiment(
    httpClient: HttpClient,
    apiKey: String,
    logger: ExperimentLogger,
    outputPath: String,
    loadTestResource: (String) -> String,
): FullExperimentResult {
    val results = mutableListOf<ExperimentResult>()

    // Case 1: standalone short dialog
    val case1 = parseTestData("case_1_short", loadTestResource("case_1_short.txt"))
    logger.logMilestone("Starting experiment: case_1_short")
    val case1Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case1.systemPrompt),
        logger = logger,
    ).runExperiment(case1)
    results.add(case1Result)
    logger.logMilestone("Finished case_1_short — peakTokens=${case1Result.peakTokens}, failed=${case1Result.failedSteps}")

    // Case 2: standalone long dialog (~27K tokens)
    val case2 = parseTestData("case_2_long", loadTestResource("case_2_long.txt"))
    logger.logMilestone("Starting experiment: case_2_long")
    val case2Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case2.systemPrompt),
        logger = logger,
    ).runExperiment(case2)
    results.add(case2Result)
    logger.logMilestone("Finished case_2_long — peakTokens=${case2Result.peakTokens}, failed=${case2Result.failedSteps}")

    // Case 3: continuation of case_2 to reach ~128K tokens
    val case3 = parseTestData("case_3_overflow", loadTestResource("case_3_overflow.txt"))
    logger.logMilestone("Starting experiment: case_3_overflow_fixed (continuation of case_2)")
    val case3Result = Day8ExperimentRunner(
        agent = Day8Agent(httpClient, apiKey, case2.systemPrompt),
        logger = logger,
    ).runOverflowExperiment(case2 = case2, case3 = case3)
    results.add(case3Result)
    logger.logMilestone("Finished case_3_overflow_fixed — peakTokens=${case3Result.peakTokens}, failed=${case3Result.failedSteps}")

    val fullResult = FullExperimentResult(cases = results, generatedAt = System.currentTimeMillis())
    saveResults(fullResult, outputPath)
    logger.logMilestone("Results saved to $outputPath")

    return fullResult
}

fun saveResults(result: FullExperimentResult, outputPath: String) {
    val file = File(outputPath)
    file.parentFile?.mkdirs()
    file.writeText(outputJson.encodeToString(result))
}
