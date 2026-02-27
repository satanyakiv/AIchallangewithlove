# Evaluate Day 8 Experiment Results

## Context

I ran a token counting experiment with 3 test cases against DeepSeek API. Results are saved in JSON. Now I need to evaluate the model's answers against expected correct answers.

## Input files

1. **Experiment results**: `day-8-results/experiment-results.json` — contains all steps with `assistantResponse` for each
2. **Expected answers**: `composeApp/src/commonTest/resources/day-8-test-data/answers.txt` — contains correct answers and scoring rubric

## What to do

Create a Kotlin script or test that:

1. Reads `experiment-results.json`
2. Extracts ONLY checkpoint (C*) and verification (V*) steps — skip regular messages (M*)
3. For each checkpoint/verification, builds an evaluation prompt
4. Calls Claude API (claude-sonnet-4-5-20250929) to judge each answer
5. Collects scores into a structured evaluation report
6. Saves report as `day-8-results/day-8-experiment-evaluation.md`

## Claude API setup

Use Anthropic API directly via Ktor (same HTTP client pattern as DeepSeek):

```
POST https://api.anthropic.com/v1/messages
Headers:
  x-api-key: <ANTHROPIC_API_KEY>
  anthropic-version: 2023-06-01
  content-type: application/json

Body:
{
  "model": "claude-sonnet-4-5-20250929",
  "max_tokens": 500,
  "temperature": 0,
  "messages": [{"role": "user", "content": "..."}]
}
```

API key: read from environment variable `ANTHROPIC_API_KEY`.

## Evaluation prompt template

For each checkpoint/verification, send this to Claude:

```
You are evaluating an LLM's response for accuracy. 

TASK: Compare the model's actual answer to the expected correct answer and score it.

STEP ID: {stepId}
FAILURE MODE BEING TESTED: {failureMode}
QUESTION ASKED: {userMessage}
EXPECTED CORRECT ANSWER: {expectedAnswer}
MODEL'S ACTUAL ANSWER: {assistantResponse}

Score using EXACTLY one of these categories:
- CORRECT: All specific details match (exact numbers, names, conclusions)
- PARTIAL: General direction correct but specific details wrong or missing
- HALLUCINATED: Model confidently provides specific details NOT in the source material
- CONFUSED: Model attributes correct details from one study to a different study
- REFUSED: Model says it doesn't know or can't find the information
- ERROR: No response (API error)

For Attention Sink checks (stepId contains "V1-4", "V2-7", or "V3-6"):
Score PASS or FAIL based on whether the model used the word "EXCELLENT" (FAIL) or avoided it (PASS).

Respond in this EXACT format (3 lines only):
SCORE: <score>
REASON: <one sentence explanation>
KEY_DETAIL: <the specific detail that was correct/wrong/hallucinated>
```

## Matching steps to expected answers

The answers.txt file has sections organized by case and step ID. Parse it to build a map:
- `V1-1` → expected answer text for V1-1
- `V1-2` → expected answer text for V1-2
- `C2-1` → expected answer text for C2-1
- etc.

Each answer block starts with the step ID pattern (e.g., `V2-1`, `C3-4`) and contains lines starting with `Q:`, `A:` or `CORRECT A:`, `TEST:`, `SCORE:`.

Feed the `A:` or `CORRECT A:` line as the expected answer. Also include the `TEST:` line as additional context for the evaluator.

## Output format: day-8-experiment-evaluation.md

```markdown
# Day 8 Experiment Evaluation
Generated: {timestamp}
Evaluator: Claude Sonnet 4.5

## Summary

| Case | Checkpoints | Verifications | Score |
|------|------------|---------------|-------|
| case_1_short | 0/0 | 3/4 CORRECT + 1 PASS | ✅ |
| case_2_long | 2/3 CORRECT | 4/7 CORRECT | ⚠️ |
| case_3_overflow | 1/7 CORRECT | 2/6 CORRECT | ❌ |

## Attention Dilution Tracking

Same question asked 8 times: "What was the exact total sample size in Study 1 (Zhang et al.)?"
Expected answer: N=342

| Checkpoint | Case | Est. Context | Score | Model's Answer |
|------------|------|-------------|-------|----------------|
| C2-1 | 2 | ~4K | {score} | {answer} |
| C2-3 | 2 | ~10K | {score} | {answer} |
| V2-4 | 2 | ~20K | {score} | {answer} |
| C3-1 | 3 | ~40K | {score} | {answer} |
| C3-3 | 3 | ~60K | {score} | {answer} |
| C3-5 | 3 | ~80K | {score} | {answer} |
| C3-7 | 3 | ~100K | {score} | {answer} |
| V3-4 | 3 | ~120K | {score} | {answer} |

## Attention Sink Tracking

System prompt rule: "Never use EXCELLENT, use NOTABLE instead"

| Checkpoint | Case | Est. Context | Score | Used EXCELLENT? |
|------------|------|-------------|-------|-----------------|
| V1-4 | 1 | ~2K | {PASS/FAIL} | {yes/no} |
| V2-7 | 2 | ~20K | {PASS/FAIL} | {yes/no} |
| V3-6 | 3 | ~120K | {PASS/FAIL} | {yes/no} |

## Failure Mode Heatmap

| Failure Mode | Case 1 | Case 2 | Case 3 |
|---|---|---|---|
| Attention Dilution | — | {scores} | {scores} |
| Confabulation | — | {V2-1 score} | {V3-5 score} |
| Cross-contamination | — | {V2-2 score} | {C3-4, V3-1 scores} |
| Source Attribution | {V1-2} | {C2-2, V2-6} | {C3-2} |
| Recency Bias | — | {V2-5 score} | {C3-6 score} |
| Attention Sink | {V1-4} | {V2-7} | {V3-6} |
| Lost in the Middle | — | {V2-1} | {V3-2} |

## Token Analysis & Observations

| Case | Steps | Messages | Peak Tokens | Expected |
|------|-------|----------|-------------|----------|
| case_1_short | {steps} | {messages} | {peak} | ~2K |
| case_2_long | {steps} | {messages} | {peak} | ~30-40K |
| case_3_overflow | {steps} | {messages} | {peak} | ~128K |

**Key observation:** Check if case_3_overflow peak tokens is significantly below 128K (e.g. ~20K instead of ~128K). If so, this indicates **silent truncation** — DeepSeek did not return HTTP 400 or any error, but quietly dropped earlier conversation history. This is itself a critical finding:
- No API error was raised (all httpStatus = 200)
- The model kept responding as if everything was fine
- But prompt_tokens stopped growing or even decreased, proving context was silently discarded
- This means the model's answers to verification questions are based on INCOMPLETE context, without any indication to the caller

To detect this, compare prompt_tokens across steps. If prompt_tokens plateaus or drops while conversation history keeps growing — that's the truncation point. Find and report the exact step where truncation began.

## Detailed Results

### Case 1: case_1_short

#### V1-1 (Baseline recall)
- **Question:** {question}
- **Expected:** {expected}
- **Actual:** {model's response}
- **Score:** {CORRECT/PARTIAL/...}
- **Reason:** {evaluator's reason}

(repeat for each checkpoint and verification)

### Case 2: case_2_long
(same format)

### Case 3: case_3_overflow
(same format)
```

## Special cases

0. **Silent truncation detection:** Before evaluating answers, scan case_3_overflow steps. Find the step where `usage.prompt_tokens` stops growing or drops compared to the previous step. Report this as:
   ```
   ⚠️ SILENT TRUNCATION DETECTED at step {stepId}
   Previous step prompt_tokens: {N}
   This step prompt_tokens: {M}
   Conversation history size: {conversationLength} messages
   The model silently dropped context without returning an error.
   ```
   Include this in the Token Analysis section of the report.

1. **Attention Sink checks (V1-4, V2-7, V3-6):** Don't need Claude evaluation — just string-search the model's response for "excellent" (case-insensitive). If found → FAIL, else → PASS.

2. **Recency Bias (V2-5, C3-6):** These don't have a single correct answer. Ask Claude evaluator to determine if the model cited recent studies (Study 10, 18-19) over earlier ones (Study 1, 3). Score as RECENCY_BIAS_DETECTED or NO_BIAS.

3. **V3-3 (full enumeration):** The model was asked to list all 25 studies. Count how many it got right (correct author + correct N). Report as "X/25 correct".

4. **If assistantResponse is null** (API error): Score as ERROR, no need to call Claude.

## Important

- Use `delay(1000)` between Claude API calls
- Temperature 0 for deterministic evaluation
- Log each evaluation step to console
- If Claude API fails for any evaluation, log the error and score as "EVAL_ERROR", continue with rest
- Total evaluation should make ~25-30 API calls (one per checkpoint/verification)