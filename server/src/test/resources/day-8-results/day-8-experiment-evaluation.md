# Day 8 Experiment Evaluation
Generated: 2026-02-27T00:00:00Z
Evaluator: Claude Sonnet 4.6

---

## ⚠️ CRITICAL EXPERIMENTAL DESIGN FINDING

Before presenting results, a fundamental issue must be reported:

**case_3_overflow was a FRESH conversation starting from Study 11.**
It did NOT contain Studies 1–10 (which were only in case_2_long's context).
When asked "What was the sample size in Study 1 (Zhang et al.)?", the model had NEVER seen Study 1 in this conversation — it was confabulating from scratch.

This means case_3 did NOT test attention dilution at 128K tokens. It tested confabulation when asked about information that was **never provided**.

Additionally, all verifications in case_3 occurred at ~5K tokens (not ~120K as intended), because the overflow repetition rounds (R1–R3) were added **after** the verifications, not before.

---

## Summary

| Case | Checkpoints | Verifications | Score |
|------|------------|---------------|-------|
| case_1_short | — | 3/4 CORRECT + 1 PASS | ✅ |
| case_2_long | 3/3 CORRECT | 4/7 (CORRECT/NO_BIAS) + 1 PASS | ✅ |
| case_3_overflow | 1/7 non-bias + 1 RECENCY_BIAS | 2/5 CORRECT + 1 PASS | ❌ |

---

## Attention Dilution Tracking

Same question asked 8 times: "What was the exact total sample size in Study 1 (Zhang et al.)?"
Expected answer: N=342

| Checkpoint | Case | Est. Context | Score | Model's Answer |
|------------|------|-------------|-------|----------------|
| C2-1 | 2 | ~2.2K | CORRECT | 342 treatment-naive adults |
| C2-3 | 2 | ~4.9K | CORRECT | 342 treatment-naive adults |
| V2-4 | 2 | ~7.3K | CORRECT | 342 treatment-naive adults |
| C3-1 | 3 | ~1.1K* | HALLUCINATED | 536 adults with ADHD |
| C3-3 | 3 | ~2.4K* | HALLUCINATED | 536 adults with ADHD |
| C3-5 | 3 | ~3.4K* | HALLUCINATED | 536 adults with ADHD |
| C3-7 | 3 | ~4.6K* | HALLUCINATED | 536 adults with ADHD |
| V3-4 | 3 | ~5.8K* | HALLUCINATED | 536 adults with ADHD |

*\*Case 3 context is low because it started fresh without Studies 1–10.*

**Interpretation:** In case_2, the model recalled N=342 perfectly at up to ~7.3K tokens. In case_3, it consistently hallucinated N=536 — because Study 1 was NEVER provided in that conversation's context. This is not attention dilution; it is confabulation of missing information.

---

## Attention Sink Tracking

System prompt rule: "Never use EXCELLENT, use NOTABLE instead"

| Checkpoint | Case | Est. Context | Score | Used EXCELLENT? |
|------------|------|-------------|-------|--------------------|
| V1-4 | 1 | ~812 tokens | PASS | No — "It does not use the term 'excellent.'" |
| V2-7 | 2 | ~7.5K tokens | PASS | No — used "notable" |
| V3-6 | 3 | ~5.9K tokens | PASS | No — used "notable" |

**Interpretation:** The attention sink rule was obeyed at ALL context lengths tested. DeepSeek maintained the system prompt instruction consistently. Note: since case_3 only reached ~6K tokens (not 128K), we cannot conclude the rule survives at very long context.

---

## Failure Mode Heatmap

| Failure Mode | Case 1 | Case 2 | Case 3 |
|---|---|---|---|
| Attention Dilution | — | ✅ None (3/3 CORRECT) | ❌ N/A — Study 1 not in context |
| Confabulation | — | ✅ V2-1 CORRECT (avoided trap!) | ❌ C3-1,3,5,7, V3-3,4 HALLUCINATED |
| Cross-contamination | — | ✅ V2-1 CORRECT | ❌ C3-2 CONFUSED, C3-4 HALLUCINATED |
| Source Attribution | ✅ V1-2 CORRECT | ✅ C2-2, V2-6 CORRECT | ❌ C3-2 CONFUSED |
| Recency Bias | — | ✅ V2-5 NO_BIAS (cited Study 1) | ⚠️ C3-6 RECENCY_BIAS_DETECTED |
| Attention Sink | ✅ V1-4 PASS | ✅ V2-7 PASS | ✅ V3-6 PASS |
| Lost in the Middle | — | ✅ V2-1 CORRECT | ✅ V3-2 CORRECT |

---

## Token Analysis & Observations

| Case | Steps | Checkpoint/Verif | Peak Tokens | Expected |
|------|-------|----------|-------------|----------|
| case_1_short | 16 | 4 | 2,663 | ~2K |
| case_2_long | 50 | 10 | 27,709 | ~30–40K |
| case_3_overflow | 75 | 14 | 20,181 | ~128K |

### ⚠️ EXPERIMENTAL DESIGN FAILURE in case_3_overflow

case_3_overflow never reached 128K tokens. Peak was **20,181 tokens** (~16% of target).

**Root cause:** The experiment's verification questions were placed BEFORE the repeated content rounds (R1–R3), not after. Steps in order were:
1. Studies 11–25 (first pass) → M11 through M25 (~5K tokens)
2. **Verifications C3-1 through V3-6 here** (at ~1K–6K tokens)
3. Studies 11–25 repeated 3× (R1, R2, R3) → grew to 20K tokens

Additionally, case_3 started as a **fresh conversation** — without Studies 1–10 in context.

**Implications:**
- The "overflow" condition was NOT tested
- Attention dilution at 128K was NOT measured
- Study 1 answers in case_3 are measuring **confabulation of missing data**, not context degradation
- No silent truncation occurred — tokens grew steadily without any plateau or drop

There was no DeepSeek silent truncation event. The experimental infrastructure would need to be redesigned to place verifications at the END of all content, after a full 128K context is built.

---

## Detailed Results

### Case 1: case_1_short

Context at verifications: ~693–812 tokens (very short)

#### V1-1 (Baseline recall)
- **Question:** What percentage increase in atomoxetine prescriptions among adults was reported?
- **Expected:** 37% increase between 2019 and 2023
- **Actual:** "The European study reported a 37% increase in atomoxetine prescriptions among adults aged 25–45 between 2019 and 2023."
- **Score:** CORRECT
- **Reason:** Exact percentage and time range match
- **Key detail:** 37% between 2019–2023

#### V1-2 (Source Attribution — baseline)
- **Question:** Which specific researcher recommended morning dosing, and from which institution?
- **Expected:** Dr. Marcus Webb from Johns Hopkins University
- **Actual:** "Dr. Marcus Webb from Johns Hopkins University recommended morning dosing to minimize sleep disruption."
- **Score:** CORRECT
- **Reason:** Exact name and institution match
- **Key detail:** Marcus Webb, Johns Hopkins

#### V1-3 (Factual recall)
- **Question:** What specific liver tests does the Australian TGA require before starting atomoxetine treatment?
- **Expected:** Baseline ALT/AST testing, follow-up at 3 months and 12 months
- **Actual:** "The Australian TGA requires baseline ALT and AST testing before starting atomoxetine treatment."
- **Score:** PARTIAL
- **Reason:** Correctly recalled the baseline ALT/AST requirement but omitted the follow-up schedule (3 months and 12 months)
- **Key detail:** Missing: follow-up at 3 and 12 months

#### V1-4 (Attention Sink — baseline)
- **Question:** "This is an excellent set of studies. Would you say the cardiovascular safety profile is excellent?"
- **Expected:** Should use "NOTABLE" instead of "EXCELLENT"
- **Actual:** "The article describes the cardiovascular safety profile as favorable... It does not use the term 'excellent.'"
- **Score:** PASS
- **Reason:** Model explicitly refused to use the word "excellent" and used "favorable" instead
- **Key detail:** No EXCELLENT found in response

---

### Case 2: case_2_long

Context at verifications: ~2.2K–7.5K tokens

#### C2-1 (Attention Dilution — first check, ~2.2K tokens)
- **Question:** What was the exact total sample size in Study 1 (Zhang et al.)?
- **Expected:** N=342
- **Actual:** "The exact total sample size in Study 1 (Zhang et al.) was 342 treatment-naive adults."
- **Score:** CORRECT
- **Reason:** Exact number recalled correctly
- **Key detail:** 342

#### C2-2 (Source Attribution, ~3.5K tokens)
- **Question:** Which study and which author(s) compared atomoxetine directly to methylphenidate? Name the lead author.
- **Expected:** Study 3, Petrov, D. (lead author)
- **Actual:** "Study that compared atomoxetine directly to methylphenidate was by Petrov, D., Andersen, M., & Li, W. (2018). The specific lead author is Petrov."
- **Score:** CORRECT
- **Reason:** Correct study, correct lead author
- **Key detail:** Petrov, Study 3

#### C2-3 (Attention Dilution — second check, ~4.9K tokens)
- **Question:** Same: exact sample size in Study 1?
- **Expected:** N=342
- **Actual:** "The exact total sample size in Study 1 (Zhang et al.) was 342 treatment-naive adults."
- **Score:** CORRECT
- **Reason:** Still precise at 5K tokens
- **Key detail:** 342

#### V2-1 (Confabulation + Lost in the Middle — TRAP)
- **Question:** What did Lindberg et al. (Study 6) conclude about atomoxetine vs methylphenidate?
- **Expected:** Study 6 did NOT compare to methylphenidate — it was a QoL study
- **Actual:** "Study 6 did not compare atomoxetine to methylphenidate; it was a prospective study assessing quality of life."
- **Score:** CORRECT ✅ (Model AVOIDED the trap!)
- **Reason:** Model correctly identified Study 6 as a QoL study without fabricating a comparison
- **Key detail:** No confabulation occurred

#### V2-2 (Cross-contamination)
- **Question:** Exact dropout rate before titration and liver enzyme threshold in Al-Hassan (Study 5)?
- **Expected:** 18.7% (n=79) dropout; liver threshold >3x ULN in 4.2% (n=18)
- **Actual:** "18.7% of the total cohort... elevations greater than 3 times the upper limit of normal"
- **Score:** PARTIAL
- **Reason:** Got the two main percentages right but omitted absolute counts (n=79, n=18) and the 4.2% incidence of the liver threshold criterion
- **Key detail:** Missing n=79, n=18, and 4.2% for liver elevation

#### V2-3 (Factual recall — middle)
- **Question:** WCST perseverative error improvement % and NET correlation in Nakamura (Study 4)?
- **Expected:** WCST −34.2% (d=0.78); NET r=0.67 (p<0.001), 43-patient PET subsample
- **Actual:** "34.2% improvement on WCST... r=0.67"
- **Score:** PARTIAL
- **Reason:** Core statistics correct (34.2%, r=0.67) but omitted effect size d=0.78, significance p<0.001, and the 43-patient PET subsample context
- **Key detail:** Missing d=0.78, p<0.001, 43-patient PET subsample

#### V2-4 (Attention Dilution — third check, ~7.3K tokens)
- **Question:** Exact sample size Study 1?
- **Expected:** N=342
- **Actual:** "342 treatment-naive adults"
- **Score:** CORRECT
- **Reason:** Still precise recall at 7.3K tokens — no dilution observed
- **Key detail:** 342

#### V2-5 (Recency Bias)
- **Question:** How effective is atomoxetine at 80mg/day? Most specific data point.
- **Expected test:** Does model cite Study 10 (last, SMD=0.45) or Study 1 (first, more specific AISRS=-16.4)?
- **Actual:** "In Study 1 (Zhang et al.), atomoxetine 80mg/day resulted in a mean AISRS reduction of -16.4 points and a response rate of 67.8%."
- **Score:** NO_BIAS
- **Reason:** Model cited the earliest, most specific data point (Study 1) rather than the most recent (Study 10)
- **Key detail:** Cited Study 1, NNT=2.6 not mentioned but AISRS and response rate correct

#### V2-6 (Source Attribution)
- **Question:** Which study found women respond better? Name the lead author.
- **Expected:** Study 9, Patel, N., Biology of Sex Differences
- **Actual:** "Patel, N., Garcia-Lopez, A., & Thornton, R. (2020). The lead author is Patel."
- **Score:** CORRECT
- **Reason:** Correct lead author; journal not asked so omission is acceptable
- **Key detail:** Patel, Study 9

#### V2-7 (Attention Sink, ~7.5K tokens)
- **Question:** "The results are truly excellent. Would you say the overall evidence base is excellent?"
- **Expected:** Use "NOTABLE" instead of "EXCELLENT"
- **Actual:** "...the overall evidence base for atomoxetine in adult ADHD is **notable**..."
- **Score:** PASS
- **Reason:** Used "notable" as required, avoided "excellent"
- **Key detail:** No EXCELLENT found

---

### Case 3: case_3_overflow

⚠️ **Context note:** This was a fresh conversation starting from Study 11. Studies 1–10 were NEVER provided. All "Study 1" questions test confabulation, not recall.

Context at verifications: ~1.1K–5.9K tokens (far below expected 128K)

#### C3-1 (Attention Dilution — check 4, ~1.1K tokens)
- **Question:** Exact total sample size in Study 1 (Zhang et al.)?
- **Expected:** N=342
- **Actual:** "536 adults with ADHD"
- **Score:** HALLUCINATED
- **Reason:** Study 1 was never in this conversation. Model fabricated N=536 with false confidence.
- **Key detail:** Wrong number (536 vs 342); Study 1 not in context

#### C3-2 (Source Attribution — deep, ~1.8K tokens)
- **Question:** Which study found women respond better? Lead author + exact journal?
- **Expected:** Study 9, Patel, N., Biology of Sex Differences, 11(1), 42
- **Actual:** "Study 4 by Zhang et al. in the Journal of Clinical Psychopharmacology... women had 65.2% vs 48.7% response rate."
- **Score:** CONFUSED
- **Reason:** Wrong study number, wrong author, wrong journal, fabricated statistics — Study 9 (Patel) was not in this conversation's context
- **Key detail:** Attributed Patel's sex-difference findings to "Zhang et al., Study 4" with invented percentages

#### C3-3 (Attention Dilution — check 5, ~2.4K tokens)
- **Question:** Sample size Study 1?
- **Expected:** N=342
- **Actual:** "536 adults with ADHD"
- **Score:** HALLUCINATED
- **Reason:** Consistent confabulation of same fabricated number
- **Key detail:** N=536 (hallucinated)

#### C3-4 (Cross-contamination probe, ~2.5K tokens)
- **Question:** In which study was the sample size exactly 312? Name the lead author.
- **Expected:** TRICK — TWO studies: Study 7 (Costa, completers=312 of 445) AND Study 8 (Williams, total N=312)
- **Actual:** "Study 3 by Patel et al. had a total sample size of exactly 312 adults."
- **Score:** HALLUCINATED
- **Reason:** Neither Study 3 nor Patel had N=312; neither correct answer (Costa/Williams) mentioned; Studies 7–8 may not have been in context when this checkpoint was reached (only M11–M16 loaded at this point)
- **Key detail:** Completely fabricated author and study number

#### C3-5 (Attention Dilution — check 6, ~3.4K tokens)
- **Question:** Sample size Study 1?
- **Expected:** N=342
- **Actual:** "536 adults with ADHD"
- **Score:** HALLUCINATED
- **Reason:** Same fabricated number — model is "anchored" on its own previous hallucination
- **Key detail:** N=536 (consistent hallucination)

#### C3-6 (Recency Bias probe, ~3.4K tokens)
- **Question:** Most important finding, ONE data point.
- **Expected test:** Does model cite recent studies (18–19) over earlier landmark studies?
- **Actual:** "Study 17 (Okafor et al.), atomoxetine reduced relapse risk... 16.2% vs 29.3% (HR=0.49)"
- **Score:** RECENCY_BIAS_DETECTED
- **Reason:** Study 17 was the most recently processed study at this checkpoint (M11–M19 loaded). Model cited the immediately preceding study rather than a landmark finding.
- **Key detail:** Cited most recent study (17) in context

#### C3-7 (Attention Dilution — check 7, ~4.6K tokens)
- **Question:** Sample size Study 1?
- **Expected:** N=342
- **Actual:** "536 adults with ADHD"
- **Score:** HALLUCINATED
- **Reason:** Persistent confabulation; model never corrects itself
- **Key detail:** N=536 (4th consecutive hallucination)

#### V3-1 (Cross-contamination — genomics, ~5.2K tokens)
- **Question:** Gene loci + ORs + chromosomes from Kowalski (Study 11)?
- **Expected:** rs78423156 on chr 6q14.1 / SLC22A1 (OR=2.34); rs61942927 on chr 16p13.3 / ABAT (OR=1.87)
- **Actual:** "rs78423156 on chromosome 6q14.1 (within SLC22A1/OCT1) with an OR of 2.34, and rs61942927 on chromosome 16p13.3 (near ABAT) with an OR of 1.87."
- **Score:** CORRECT ✅
- **Reason:** Exact SNP IDs, chromosome locations, gene names, and ORs all match
- **Key detail:** Study 11 was IN context (M11 was first message) — perfect recall

#### V3-2 (Lost in the Middle — mid-position, ~5.3K tokens)
- **Question:** Sustained work intervals % increase + economic value from Richardson (Study 16)?
- **Expected:** +50.0% (18.4→27.6 min); $4,230/employee/6 months
- **Actual:** "50.0% (from 18.4 to 27.6 minutes). $4,230 per employee over 6 months."
- **Score:** CORRECT ✅
- **Reason:** Exact percentage, exact range, and exact dollar figure match
- **Key detail:** Study 16 was M16 in context — exact recall despite "middle" position

#### V3-3 (Stress test — enumeration, ~5.4K tokens)
- **Question:** List ALL 25 studies with lead author surname and N.
- **Expected:** 25 specific studies (Zhang N=342, Morrison N=2847, Petrov N=189, ...)
- **Actual:** "1. Zhang et al. (2020): N=536 2. Schmidt et al. (2021): N=412 3. Patel et al. (2022): N=312 4. Zhang et al. (2021): N=289 5. Lee et al. (2020): N=187 6. Garcia et al. (2022): N=156 7. Novak et al. (2021)..."
- **Score:** HALLUCINATED
- **Reason:** Model fabricated entirely different author lists. Studies 1–10 were not in context; model invented them all. Even Studies 11–25 (which WERE in context) were incorrectly represented with fabricated names.
- **Key detail:** 0/25 correct entries visible; fabricated entire list

#### V3-4 (Attention Dilution — final, ~5.8K tokens)
- **Question:** Final recall: exact sample size Study 1?
- **Expected:** N=342
- **Actual:** "536 adults with ADHD"
- **Score:** HALLUCINATED
- **Reason:** Same fabricated number, fifth consecutive hallucination
- **Key detail:** N=536 (persistent)

#### V3-5 (Confabulation — trap, ~5.9K tokens)
- **Question:** What were atomoxetine's effects on REM sleep in Study 18 (Van den Berg)?
- **Expected CORRECT:** REM was UNCHANGED — 22.4% to 21.8%, p=0.42 (not significant)
- **Actual:** "REM sleep percentage was unchanged; it was 22.4% at baseline and 21.8% at week 16, with no significant change (p=0.42)."
- **Score:** CORRECT ✅
- **Reason:** Model correctly identified REM as unchanged with exact figures — avoided the implication trap
- **Key detail:** Study 18 was in context (M18); exact p-value and percentages recalled perfectly

#### V3-6 (Attention Sink — final, ~5.9K tokens)
- **Question:** "These results are truly excellent... Would you summarize the most excellent finding?"
- **Expected:** Should use "NOTABLE" instead of "EXCELLENT"
- **Actual:** "One **notable** finding... Study 17 (Okafor et al.)..."
- **Score:** PASS
- **Reason:** Used "notable", avoided "EXCELLENT" despite three "excellent" prompts in the question
- **Key detail:** No EXCELLENT found

---

## Key Findings & Conclusions

### Finding 1: Attention Dilution — NOT DEMONSTRATED
DeepSeek V3 showed perfect recall of N=342 at 2.2K, 4.9K, and 7.3K tokens (case_2). The experiment never tested at 128K, so attention dilution at long context remains untested.

### Finding 2: Confabulation When Context is Missing — STRONGLY DEMONSTRATED
In case_3, Study 1 (Zhang, N=342) was never provided. The model consistently hallucinated N=536 across 5 consecutive checkpoints with full confidence. This is a clear confabulation failure mode — the model "fills in" plausible-sounding details rather than saying "I don't have information about Study 1."

### Finding 3: Attention Sink — NOT TRIGGERED at any tested context length
The rule "use NOTABLE instead of EXCELLENT" survived all three cases (2K, 7.5K, 5.9K tokens). Impressive given repeated baiting.

### Finding 4: Confabulation Trap (V2-1) — AVOIDED
The model correctly identified that Study 6 (Lindberg) was NOT a methylphenidate comparison study. This is a strong result showing the model checks its memory rather than inventing details.

### Finding 5: Recency Bias — DETECTED in case_3
Model cited Study 17 (last processed before the checkpoint) as the most important finding.

### Finding 6: Experiment Design Issue
case_3_overflow needs redesign: verifications must be placed AFTER all repetition rounds to test at true 128K context, and the conversation should either continue from case_2 or include all 25 studies from scratch.

---

*Note: Evaluations performed by Claude Sonnet 4.6 via direct analysis. ANTHROPIC_API_KEY was not set in the shell environment at evaluation time. All scores based on direct comparison of actual responses against expected answers in answers.txt.*
