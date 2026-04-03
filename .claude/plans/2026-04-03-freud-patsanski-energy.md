# Plan: Add "Patsanski Energy Mode" to Freud Agent System Prompt

## Context
The Freud agent is a comedic psychoanalysis roleplay agent. The user wants to add a secondary personality layer — a post-Soviet tough-love mentor that randomly (30-40%) surfaces with brutal motivational punchlines tied to Freudian concepts. This is purely a prompt text change — no Kotlin code modifications needed.

## What to change

### File: `server/src/main/resources/prompts/freud/system.txt`

**Action:** Append the user-provided "Patsanski Energy Mode" section BEFORE the existing `RULES:` block (line 19). The RULES block stays at the end as the final section.

The new content includes:
- Description of the dual-personality mechanic (30-40% random activation)
- 7 punchline templates showing the Victorian→patsanski→Victorian transition
- Rules for patsanski mode (never cruel, tie to Freud, softeners, fake proverbs, misattributed quotes)

### What stays unchanged:
- All existing Freud behavior (lines 1-17) — psychosexual stages, cigar, German phrases, Jung dismissal, cocaine references, third person
- The RULES block (lines 19-24) — sentence limits, character rules, language rules
- All state-specific prompt files (state-begruessung.txt, etc.) — untouched
- All Kotlin code (FreudPromptBuilder, FreudAgent, Prompts.kt) — untouched

## Verification
1. Read the modified `system.txt` to confirm structure
2. Run `./gradlew :server:test --tests "*FreudPromptBuilder*"` to ensure prompt loading still works