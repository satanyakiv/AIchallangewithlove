Read .claude/rules/architecture.md
Read server/src/main/kotlin/com/portfolio/ai_challenge/agent/day_11_psy_agent/PsyAgent.kt — this is the REFERENCE for correct style.

## Problem

Legacy agents (Day6-Day10) create dependencies internally or receive
raw httpClient + apiKey instead of using LlmClient. They mix
orchestration with HTTP calls and prompt strings.

PsyAgent is the correct pattern: receives ready-to-use components
via constructor, does pure orchestration.

## Task

Refactor ALL agents in server/src/main/kotlin/com/portfolio/ai_challenge/agent/
to follow the PsyAgent pattern.

For each agent (Day6Agent, Day7Agent, Day9Agent, Day10SlidingAgent,
Day10FactsAgent, Day10BranchingAgent):

### Step 1: Replace httpClient + apiKey with LlmClient

Before:
```kotlin
class Day7Agent(private val httpClient: HttpClient, private val apiKey: String)
```

After:
```kotlin
class Day7Agent(private val llmClient: LlmClient)
```

Move the DeepSeek API call logic to use llmClient.complete(messages)
instead of manual httpClient.post() calls. If an agent uses different
API parameters (temperature, max_tokens), add overload parameters
to LlmClient.complete() or pass them as arguments.

### Step 2: Extract prompt strings to resources (if time permits)

If an agent has inline prompt strings, note them but do NOT extract
to resource files yet — that is a separate task. Just leave a
TODO comment: // TODO: extract to resources/prompts/dayN/

### Step 3: Update Application.kt

Update the dependency creation in Application.module() to pass
LlmClient instead of httpClient + apiKey:

Before:
```kotlin
val day7Agent = Day7Agent(httpClient, apiKey)
```

After:
```kotlin
val day7Agent = Day7Agent(llmClient)
```

The LlmClient singleton already exists in Application.kt.

### Step 4: Update route functions if their signatures changed

If route functions received httpClient/apiKey separately, update them.

## Rules

- Do NOT change any agent behavior or API responses
- Do NOT change test behavior — but update constructor calls in tests
  to match new signatures (pass LlmClient or mock instead of httpClient + apiKey)
- If an agent uses httpClient for something OTHER than DeepSeek
  (unlikely but check), keep that separate dependency
- If LlmClient.complete() needs new parameters (different temperature,
  max_tokens, model), add optional parameters with defaults to the
  existing complete() method. Do NOT create multiple LlmClient classes.

## Verification

1. ./gradlew :server:test — all tests pass
2. ./gradlew :server:run — server starts
3. Grep check: no agent file should import io.ktor.client.HttpClient
   (only LlmClient should have that import)
4. Grep check: no agent file should contain the string "DEEPSEEK_API_KEY"
   or "api.deepseek.com"hf