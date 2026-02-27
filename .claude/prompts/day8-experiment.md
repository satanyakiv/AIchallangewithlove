# Day 8 Experiment Runner — Code Generation Prompt

## Context

I'm building an AI challenge project (Android, Kotlin). Day 7 already has a working chat agent that talks to DeepSeek API. Day 8 task: measure token growth and demonstrate context window degradation.

## Existing Code

Here's my working Day7Agent:

```kotlin
package com.portfolio.ai_challenge.agent

import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.DeepSeekRequest
import com.portfolio.ai_challenge.models.DeepSeekResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiMessageDto(val role: String, val content: String)

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Day7Agent(private val httpClient: HttpClient, private val apiKey: String) {

    suspend fun chat(messages: List<ApiMessageDto>): String {
        val allMessages = buildList {
            add(DeepSeekMessage(role = "system", content = SYSTEM_PROMPT))
            addAll(messages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = allMessages,
            temperature = 0.7,
        )
        val response = httpClient.post(DEEPSEEK_API_URL) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeepSeekRequest.serializer(), request))
        }
        val rawBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception("DeepSeek error (${response.status.value}): $rawBody")
        }
        val deepSeekResponse = json.decodeFromString<DeepSeekResponse>(rawBody)
        return deepSeekResponse.choices.first().message.content
    }
}
```

## What I Need

Create `Day8ExperimentRunner` — a class that runs the token experiment. NOT a UI test, NOT an Android instrumented test. This is a plain Kotlin class that I will call from a JUnit test OR from a ViewModel. It must work in both contexts.

## Architecture

```
Day8ExperimentRunner
  ├── parseTestData(rawText: String) -> TestCase
  ├── runExperiment(testCase: TestCase) -> ExperimentResult  
  └── internal: sends messages via modified Day7Agent that returns usage stats
```

## Step 1: Modify DeepSeek models to capture token usage

The DeepSeek API already returns a `usage` object in the response. My existing `DeepSeekResponse` model might not capture it. Ensure the response model includes:

```json
{
  "usage": {
    "prompt_tokens": 1234,
    "completion_tokens": 567,
    "total_tokens": 1801,
    "prompt_cache_hit_tokens": 0,
    "prompt_cache_miss_tokens": 1234
  }
}
```

Add a `@Serializable` data class `TokenUsage` with these fields. Add `usage: TokenUsage?` to `DeepSeekResponse`.
If DeepSeekResponse doesn't have `usage` field — add it. Don't break existing code.

## Step 2: Create Day8Agent

Create `Day8Agent` that wraps the DeepSeek API call similar to Day7Agent but:
- Accepts system prompt as constructor parameter (different per test case)
- Returns a rich result object, not just String:

```kotlin
@Serializable
data class AgentResponse(
    val content: String,
    val usage: TokenUsage?,
    val rawResponseBody: String,   // for debugging
    val httpStatus: Int,
    val errorMessage: String? = null
)
```

- Does NOT throw on API errors. Instead returns AgentResponse with errorMessage filled.
- Logs every step (see logging section below).

## Step 3: Test Data Parser

Parse the test data files. Format is sections separated by `=== HEADER ===` lines.

Section types identified by header prefix:
- `SYSTEM PROMPT` → system prompt text
- `NOTE FOR AUTOTEST` → skip (metadata for humans)
- `MESSAGE N` → user message to send to model (article + question)
- `CHECKPOINT C...` → intermediate verification question (also sent as user message)
- `VERIFICATION V...` → final verification question (sent as user message)

Parse into:

```kotlin
data class TestCase(
    val name: String,                     // "case_1_short", "case_2_long", "case_3_overflow"
    val systemPrompt: String,
    val steps: List<TestStep>             // ordered sequence of messages + checkpoints + verifications
)

sealed class TestStep {
    data class Message(
        val id: String,                   // "MESSAGE 1", "MESSAGE 11"
        val content: String               // full text including article + question
    ) : TestStep()

    data class Checkpoint(
        val id: String,                   // "C2-1", "C3-4"
        val failureMode: String,          // extracted from parenthetical, e.g. "Attention Dilution"
        val content: String               // the question text
    ) : TestStep()

    data class Verification(
        val id: String,                   // "V2-1", "V3-5"
        val failureMode: String,          // extracted from parenthetical
        val content: String               // the question text
    ) : TestStep()
}
```

Parser rules:
- Split file by lines starting with `=== ` and ending with ` ===`
- Header between `=== ` and ` ===` determines type
- `MESSAGE \d+` → TestStep.Message
- `CHECKPOINT (C[\w-]+) \((.+)\)` → TestStep.Checkpoint with id=$1, failureMode=$2
- `VERIFICATION (V[\w-]+) \((.+)\)` → TestStep.Verification with id=$1, failureMode=$2
- Content is everything between current header and next header, trimmed
- System prompt: content after `=== SYSTEM PROMPT ===`
- Skip sections starting with `NOTE FOR AUTOTEST`

## Step 4: Experiment Runner

```kotlin
class Day8ExperimentRunner(
    private val agent: Day8Agent,
    private val logger: ExperimentLogger
) {
    suspend fun runExperiment(testCase: TestCase): ExperimentResult
}
```

Algorithm:

```
conversationHistory = mutableListOf<ApiMessageDto>()
stepResults = mutableListOf<StepResult>()

for each step in testCase.steps:
    logger.logStep(step, conversationHistory.size)
    
    // Add user message to history
    conversationHistory.add(ApiMessageDto(role = "user", content = step.content))
    
    // Call API
    response = agent.chat(systemPrompt = testCase.systemPrompt, messages = conversationHistory)
    
    logger.logResponse(response)
    
    // Add assistant response to history  
    if (response.errorMessage == null) {
        conversationHistory.add(ApiMessageDto(role = "assistant", content = response.content))
    }
    
    // Record result
    stepResults.add(StepResult(
        stepId = step.id,
        stepType = step.type,           // "message" | "checkpoint" | "verification"
        failureMode = step.failureMode, // null for regular messages
        userMessage = step.content,
        assistantResponse = response.content,
        usage = response.usage,
        httpStatus = response.httpStatus,
        errorMessage = response.errorMessage,
        conversationLength = conversationHistory.size,
        timestamp = System.currentTimeMillis()
    ))
    
    // If API error in overflow test — log it and stop, that's a valid result
    if (response.errorMessage != null) {
        logger.logError("API error at step ${step.id}: ${response.errorMessage}")
        // DON'T break for case 3 — the error IS the result we want to capture
        // But DO stop sending more messages since conversation is broken
        break
    }
    
    // Small delay to avoid rate limiting
    delay(1000)

return ExperimentResult(
    caseName = testCase.name,
    steps = stepResults,
    totalMessages = conversationHistory.size,
    peakTokens = stepResults.maxOfOrNull { it.usage?.totalTokens ?: 0 } ?: 0
)
```

## Step 5: Result Models

```kotlin
@Serializable
data class StepResult(
    val stepId: String,
    val stepType: String,              // "message", "checkpoint", "verification"  
    val failureMode: String? = null,   // "Attention Dilution", "Confabulation", etc.
    val userMessage: String,
    val assistantResponse: String?,
    val usage: TokenUsage?,
    val httpStatus: Int,
    val errorMessage: String? = null,
    val conversationLength: Int,       // number of messages in history at this point
    val timestamp: Long
)

@Serializable
data class ExperimentResult(
    val caseName: String,
    val steps: List<StepResult>,
    val totalMessages: Int,
    val peakTokens: Int
)

@Serializable
data class FullExperimentResult(
    val timestamp: Long,
    val cases: List<ExperimentResult>
)
```

## Step 6: Logging

Create `ExperimentLogger` interface + implementation. This is critical for debugging.

```kotlin
interface ExperimentLogger {
    fun logStep(step: TestStep, historySize: Int)
    fun logResponse(response: AgentResponse)
    fun logError(message: String)
    fun logMilestone(message: String)
}
```

Implementation should use Android `Log` when running on device, or `println` when in JUnit.

Log format for each step:
```
═══════════════════════════════════════════════
[STEP] MESSAGE 5 | History: 9 messages
[SENT] "Study 5. Al-Hassan, R., Dubois, J..." (first 80 chars)
───────────────────────────────────────────────
[RECV] Status: 200 | Tokens: prompt=4523 completion=187 total=4710
[RECV] Cache: hit=3200 miss=1323
[RESP] "The dropout rate before completing..." (first 120 chars)
═══════════════════════════════════════════════
```

For checkpoints/verifications, add visual distinction:
```
★★★ [CHECKPOINT C2-1] Attention Dilution ★★★
[SENT] "Quick recall check: What was the exact..."
[RECV] Status: 200 | Tokens: prompt=8234 completion=45 total=8279
[RESP] "The sample size in Study 1 was 342."
★★★ END CHECKPOINT ★★★
```

For errors:
```
✖✖✖ [ERROR] at step MESSAGE 23 ✖✖✖
HTTP 400: {"error":{"message":"This model's maximum context length is 131072 tokens..."}}
✖✖✖ Experiment stopped ✖✖✖
```

## Step 7: Orchestrator

Create a top-level function that runs all 3 cases:

```kotlin
suspend fun runFullExperiment(
    httpClient: HttpClient,
    apiKey: String,
    logger: ExperimentLogger,
    loadTestResource: (String) -> String  // lambda to load from resources
): FullExperimentResult {
    val agent = Day8Agent(httpClient, apiKey)
    val runner = Day8ExperimentRunner(agent, logger)
    
    val cases = listOf("case_1_short", "case_2_long", "case_3_overflow")
    val results = mutableListOf<ExperimentResult>()
    
    for (caseName in cases) {
        logger.logMilestone("Starting $caseName")
        
        val rawText = loadTestResource("day-8-test-data/$caseName.txt")
        val testCase = parseTestData(caseName, rawText)
        
        logger.logMilestone("Parsed ${testCase.steps.size} steps for $caseName")
        
        val result = runner.runExperiment(testCase)
        results.add(result)
        
        logger.logMilestone("Completed $caseName: ${result.totalMessages} messages, peak ${result.peakTokens} tokens")
        
        // Pause between cases
        delay(3000)
    }
    
    return FullExperimentResult(
        timestamp = System.currentTimeMillis(),
        cases = results
    )
}
```

## Step 8: Saving Results

Save `FullExperimentResult` as JSON to a file. This JSON will later be:
1. Read by the dashboard UI
2. Sent to Claude Opus for evaluation (separate step, not in this prompt)

```kotlin
fun saveResults(result: FullExperimentResult, outputPath: String) {
    val jsonString = Json { prettyPrint = true }.encodeToString(result)
    File(outputPath).writeText(jsonString)
}
```

## Case 3 Special Handling: Overflow Loop

For case 3, the test data file has a NOTE saying to repeat studies if context is under 128K.
Implement this in the runner:

After processing all steps from case_3_overflow.txt, check `peakTokens`:
- If peakTokens < 120_000 AND no API error occurred:
    - Re-send messages from the beginning with prefix "REPEAT: " added to content
    - Continue until API error OR peakTokens > 125_000
    - Maximum 3 repetition cycles to avoid infinite loop
- Log each repetition cycle clearly

## Important Constraints

- DO NOT use `runBlocking` in production code. Use `suspend` functions throughout.
- DO NOT hardcode file paths. Accept them as parameters.
- DO NOT swallow exceptions silently. Always log.
- DO NOT use Android-specific APIs in the core logic (keep it testable from JUnit).
- DO keep serialization compatible with existing `DeepSeekRequest`/`DeepSeekResponse` models.
- DO use `delay(1000)` between API calls to respect rate limits.
- Temperature should be 0.3 (lower than Day7's 0.7 — we want more deterministic responses for testing).

## File Structure

```
com.portfolio.ai_challenge/
├── agent/
│   ├── Day7Agent.kt          (existing, don't modify)
│   └── Day8Agent.kt          (new)
├── experiment/
│   ├── Day8ExperimentRunner.kt
│   ├── TestDataParser.kt
│   ├── ExperimentLogger.kt
│   └── models/
│       ├── TestCase.kt
│       ├── StepResult.kt
│       └── ExperimentResult.kt
├── models/
│   ├── DeepSeekRequest.kt     (existing)
│   ├── DeepSeekResponse.kt    (modify: add TokenUsage)
│   └── TokenUsage.kt          (new)
```

## Test Data Files Location

Test data files are already placed at:
`composeApp/src/commonTest/resources/day-8-test-data/`

Contents:
- `case_1_short.txt`
- `case_2_long.txt`
- `case_3_overflow.txt`
- `answers.txt`

To read them in commonTest, use:
```kotlin
val text = this::class.java.classLoader!!
    .getResource("day-8-test-data/case_1_short.txt")!!
    .readText()
```

The parser should work with raw String input so it doesn't care about file location.

## Summary of Deliverables

1. `TokenUsage.kt` — serializable model for DeepSeek usage stats
2. Modify `DeepSeekResponse.kt` — add `usage: TokenUsage?` field
3. `Day8Agent.kt` — API wrapper returning rich responses
4. `TestDataParser.kt` — parses test case files into structured data
5. `TestCase.kt`, `StepResult.kt`, `ExperimentResult.kt` — data models
6. `ExperimentLogger.kt` — interface + println implementation
7. `Day8ExperimentRunner.kt` — runs one test case
8. Top-level `runFullExperiment()` function — orchestrates all 3 cases
9. JSON output saved to file