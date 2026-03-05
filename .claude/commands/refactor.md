Read .claude/rules/architecture.md, .claude/rules/prompts.md, .claude/rules/testing.md

## Project context

MindGuard psy-agent. Code: server/.../agent/day_11_psy_agent/.
Layered: Routes → Agent → UseCases → Store.
Prompts in resources/prompts/psy/. Single Prompts object in agent/Prompts.kt.

## Refactor

$ARGUMENTS

## Process

1. **AUDIT**: Read all affected files. List what violates the rule
   or what needs restructuring. Show me:
    - Files to change
    - What moves where
    - What gets extracted/merged/renamed
      **Wait for my "go".**

2. **REFACTOR**: Apply changes. Zero behavior changes.

3. **VERIFY**: Run all related tests:
   `./gradlew :server:test --tests "*RelevantTest"`
   All must pass. If any fail — the refactor broke something, revert and retry.

4. **CHECKLIST**: Show post-refactor state:
    - No file > 150 lines
    - No function > 20 lines
    - No prompt strings in .kt files
    - No HTTP imports in Agent class
    - No duplicate models

## Rules
- ZERO behavior changes. Same inputs → same outputs.
- Never run ./gradlew test without --tests filter.
- Do NOT break existing tests.
- If unsure about a change — ask, don't guess.