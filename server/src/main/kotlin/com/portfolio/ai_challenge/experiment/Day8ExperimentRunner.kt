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
}

suspend fun runFullExperiment(
    httpClient: HttpClient,
    apiKey: String,
    logger: ExperimentLogger,
    outputPath: String,
    loadTestResource: (String) -> String,
): FullExperimentResult {
    val caseFiles = listOf("case_1_short.txt", "case_2_long.txt", "case_3_overflow.txt")
    val results = mutableListOf<ExperimentResult>()

    for (fileName in caseFiles) {
        logger.logMilestone("Starting experiment: $fileName")
        val raw = loadTestResource(fileName)
        val testCase = parseTestData(name = fileName.removeSuffix(".txt"), rawText = raw)

        val day8Agent = Day8Agent(
            httpClient = httpClient,
            apiKey = apiKey,
            systemPrompt = testCase.systemPrompt,
        )
        val runner = Day8ExperimentRunner(agent = day8Agent, logger = logger)
        val result = runner.runExperiment(testCase)
        results.add(result)
        logger.logMilestone("Finished $fileName — peakTokens=${result.peakTokens}, failed=${result.failedSteps}")
    }

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
