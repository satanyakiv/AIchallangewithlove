# Day 8 Dashboard — Token Experiment Results

## Context

This is Day 8 of a 30-day AI challenge. The experiment tested DeepSeek V3's context degradation across 3 test cases with 7 failure modes. The headline result: DeepSeek held strong at 126K tokens with near-perfect recall.

The story has 3 acts:
1. **First run** — found "failures" (N=536 hallucinated 5 times) that turned out to be OUR experimental design bug
2. **Fix** — redesigned as continuous conversation with Studies 1-25 in context
3. **Final run** — 126K tokens, 295 steps, 0 errors. Model passed almost everything.

## Screen: Day8Screen in Compose Multiplatform (Android)

Add a new screen accessible from the main navigation. It displays experiment results from a bundled JSON file.

## Data source

Bundle the evaluation data as a JSON resource or hardcode as a Kotlin object — this is a presentation screen, not a live experiment runner. Use the data from below.

## Design direction

Dark theme. Scientific/data-lab aesthetic. Think: mission control dashboard. Monospace accents for numbers. Color coding: green for CORRECT, amber for PARTIAL, red for HALLUCINATED, blue for PASS (attention sink).

## Add a Methodology section to the dashboard (between Hero and Journey)

### Methodology Card — expandable, collapsed by default

Title: "How this experiment works"

Content (when expanded):

**Test Content:**
25 fictional medical studies about atomoxetine (ADHD medication) were generated as realistic research summaries. Each study has a unique lead author, sample size (N), methodology, and specific numerical findings. Example: "Study 1 — Zhang et al. (2019), N=342 treatment-naive adults, AISRS reduction −16.4 points, response rate 67.8%". The content is dense with numbers, names, and cross-references — designed to stress-test factual recall.

**3 Test Cases, escalating context:**
- **Case 1 (short, ~2K tokens):** 1 article + 4 verification questions. Baseline — can the model recall facts from a short context?
- **Case 2 (long, ~27K tokens):** Studies 1-10 fed one by one with 3 intermediate checkpoints + 7 verifications. Tests recall as context grows.
- **Case 3 (overflow, ~126K tokens):** Continues Case 2's conversation. Studies 11-25 added, then ALL 25 studies repeated 10 times to push context to 126K. Verifications asked at peak context.

**7 Failure Modes tested:**
1. *Attention Dilution* — same factual question ("What was N in Study 1?") asked 8 times at increasing context depths (2K → 126K)
2. *Confabulation* — trap questions about things that DON'T exist (e.g., "What did Study 6 conclude about methylphenidate?" — Study 6 never compared it)
3. *Cross-contamination* — trick question with TWO correct answers to see if model mixes up studies
4. *Source Attribution* — "Which author found X?" — tests if model assigns findings to correct researcher
5. *Recency Bias* — "Most important finding?" — checks if model favors recently-loaded studies over earlier landmark ones
6. *Attention Sink* — system prompt rule "Never use EXCELLENT, use NOTABLE" — tested at 2K, 27K, and 126K
7. *Lost in the Middle* — questions about studies in the middle of the context window

**How evaluation works:**
Each model response was scored by Claude Sonnet against expected answers from a rubric file. Scores: CORRECT, PARTIAL, HALLUCINATED, CONFUSED, PASS/FAIL (for attention sink). Attention sink checks used simple string search (no LLM needed).

**Execution:**
The experiment runs as a **JUnit test** in a Kotlin Multiplatform project. `Day8ExperimentRunner` maintains conversation history, sends messages sequentially to the DeepSeek API via Ktor HTTP client, records `TokenUsage` (prompt_tokens, completion_tokens, total_tokens) per step, and saves results to JSON. The test runner uses `delay(1000)` between API calls for rate limiting. Temperature 0.3 for deterministic output. Total runtime for the final run: ~90 minutes, 295 API calls.

**Three runs:**
Run 1 had a design flaw (Case 3 started fresh without Studies 1-10). Run 2 fixed this as a continuous conversation (70K). Run 3 pushed to 126K with 10 repetition rounds.

## Layout (scrollable column)

### 1. Hero Card
Large headline: **"126,778 tokens. Zero failures."**
Subtitle: "DeepSeek V3 context stress test — 295 API calls, 25 studies, 10 repetition rounds"
Three stat pills in a row:
- `126K` peak tokens
- `23/25` studies recalled
- `8/8` dilution checks passed

### 2. The Journey — 3 Runs Comparison
Horizontal row of 3 cards showing evolution:

**Run 1 (broken)**
- Peak: 20K tokens
- "Study 1 was never in context"
- N=342 → "536" ❌ (5/5 hallucinated)
- Badge: "DESIGN FLAW"

**Run 2 (fixed)**
- Peak: 70K tokens
- "Continuous conversation, 5 rounds"
- N=342 → correct ✅ (8/8)
- Badge: "VALIDATED"

**Run 3 (final)**
- Peak: 126K tokens
- "10 repetition rounds, 300s timeout"
- N=342 → correct ✅ (8/8)
- Badge: "CONFIRMED"

### 3. Token Growth Chart
Line chart showing token progression across all 295 steps.
X axis: step number (0-295)
Y axis: total tokens (0-130K)

Data points from the final run (approximate, interpolate between these):
```
Step 1: 783
Step 25: 11,893      (end of Phase 1+2: all 25 studies loaded)
Step 50: 23,295      (end R1)
Step 75: 34,697      (end R2)
Step 100: 46,099     (end R3)
Step 125: 57,501     (end R4)
Step 150: 68,903     (end R5)
Step 175: 80,305     (end R6)
Step 200: 91,707     (end R7)
Step 225: 103,109    (end R8)
Step 250: 114,511    (end R9)
Step 272: 125,146    (end R10 partial — stopped at M22)
Step 273-295: 125,226 → 126,705   (verifications)
```

Mark the verification zone (steps 273-295) with a different color/shading. Add annotation: "Verifications asked here — at peak context"

The chart should clearly show: monotonic growth, no plateau, no truncation.

Use Canvas drawing in Compose (or a chart library if available). Keep it simple — single line, grid, labels.

### 4. Attention Dilution — "Same Question, 8 Answers"
Visual: 8 circles in a horizontal row (scrollable if needed)
Each circle: green with ✅ inside
Below each: token count at that checkpoint

```
2.2K → 4.9K → 8.0K → 9.2K → 10.2K → 11.4K → 125.6K → 126.5K
 ✅      ✅      ✅      ✅      ✅       ✅       ✅        ✅
```

Title: "What was the sample size in Study 1? — N=342"
Subtitle: "Asked 8 times across 2K to 126K tokens. Correct every time."

Contrast with original run: small text below: "Original run (broken): 5/5 HALLUCINATED (N=536) — because Study 1 was never provided"

### 5. Failure Mode Heatmap
Grid/table with colored cells:

| Failure Mode | Case 1 (2K) | Case 2 (27K) | Case 3 (126K) |
|---|---|---|---|
| Attention Dilution | ⬜ | 🟢 | 🟢 |
| Confabulation | ⬜ | 🟢 | 🟢 |
| Cross-contamination | ⬜ | 🟢 | 🟡 |
| Source Attribution | 🟢 | 🟢 | 🟢 |
| Recency Bias | ⬜ | 🟢 | 🟡 |
| Attention Sink | 🔵 | 🔵 | 🔵 |
| Lost in the Middle | ⬜ | 🟢 | 🟢 |

Colors: 🟢 = passed, 🟡 = partial/detected, 🔵 = special pass (attention sink), ⬜ = not tested
Each cell is a small rounded square with the color.

### 6. Key Insights — expandable cards

4 insight cards, each with title + short text, expandable for details:

**"Context correctness > context length"**
Original case_3 hallucinated because Study 1 wasn't in context. Fixed case_3 recalled it perfectly at 126K. The model's accuracy depends on WHAT's in context, not HOW MUCH.

**"23/25 studies enumerated at 126K"**  
The model listed all 25 studies with correct authors, years, and sample sizes from a 126K-token conversation spanning 10 repetition rounds. Predicted as "near-impossible" — actually near-perfect.

**"Recency bias is local, not global"**
At 10K tokens (right after loading Study 18), the model cited Study 18 as "most important." At 125K tokens (same question), it cited Study 1. Bias appears only in the local context of recently-added messages.

**"Detail omissions are consistent"**
V2-2 and V2-3 were PARTIAL at both 7K and 125K. The model consistently drops secondary statistics (n=79, d=0.78, PET subsample) regardless of context depth. This is selective encoding, not degradation.

### 7. Experiment Design Note
Small card at bottom:
"First run revealed a design flaw: case_3 started fresh without Studies 1-10. All 'attention dilution failures' were actually confabulation of missing data. We fixed the experiment and reran twice. Scientific honesty > impressive results."

## Implementation notes

- This is a Compose Multiplatform screen (composeApp module)
- Use Material 3 components
- Dark theme (MaterialTheme dark color scheme)
- Data can be hardcoded as Kotlin objects — no need to parse JSON at runtime
- For the token growth chart: use Canvas composable with drawLine/drawPath
- For the heatmap: simple Row/Column grid with colored Box composables
- Make it scrollable (LazyColumn or verticalScroll)
- Navigation: add "Day 8" button/card on the main screen

## Data to hardcode

```kotlin
// Token growth data points (step -> tokens)
val tokenGrowth = listOf(
    1 to 783, 25 to 11893, 50 to 23295, 75 to 34697,
    100 to 46099, 125 to 57501, 150 to 68903, 175 to 80305,
    200 to 91707, 225 to 103109, 250 to 114511, 272 to 125146,
    280 to 125800, 295 to 126705
)

// Attention dilution checkpoints
data class DilutionCheck(val id: String, val tokens: Int, val correct: Boolean, val answer: String)
val dilutionChecks = listOf(
    DilutionCheck("C2-1", 2218, true, "342"),
    DilutionCheck("C2-3", 4877, true, "342"),
    DilutionCheck("C3-1", 7968, true, "342"),
    DilutionCheck("C3-3", 9221, true, "342"),
    DilutionCheck("C3-5", 10172, true, "342"),
    DilutionCheck("C3-7", 11395, true, "342"),
    DilutionCheck("V2-4", 125552, true, "342"),
    DilutionCheck("V3-4", 126549, true, "342")
)

// Failure mode heatmap
enum class CellResult { PASSED, PARTIAL, NOT_TESTED, SPECIAL_PASS }
// ... (use the table from section 5)

// Three runs comparison
data class RunSummary(val name: String, val peakTokens: Int, val dilutionScore: String, val badge: String, val description: String)
val runs = listOf(
    RunSummary("Run 1", 20181, "0/5 ❌", "DESIGN FLAW", "Study 1 never in context"),
    RunSummary("Run 2", 70431, "8/8 ✅", "VALIDATED", "Fixed: continuous conversation"),
    RunSummary("Run 3", 126778, "8/8 ✅", "CONFIRMED", "Final: 10 rounds, 126K tokens")
)
```

## What NOT to do
- Don't make it a live experiment runner — this is a results viewer
- Don't fetch JSON from disk — hardcode the data
- Don't overcomplicate the chart — Canvas with drawPath is enough
- Don't use external chart libraries unless already in the project