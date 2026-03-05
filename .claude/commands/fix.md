Read .claude/rules/architecture.md, .claude/rules/testing.md

## Project context

MindGuard psy-agent. Code: server/.../agent/day_11_psy_agent/.
Layered: Routes → Agent → UseCases → Store.
Tests mock LlmClient. Never call real API.

## Bug

$ARGUMENTS

## Process

1. **REPRODUCE**: Find the relevant code. Explain what's wrong and why.
   Show me the broken flow. Wait for my confirmation.

2. **TEST FIRST**: Write a failing test that reproduces the bug.
   Run: `./gradlew :server:test --tests "*ClassName"` to confirm it fails.

3. **FIX**: Minimal change. Do not refactor unrelated code.

4. **VERIFY**: Run the new test + all related existing tests.
   Show: what changed, what was the root cause, how the fix works.

## Rules
- Mock LlmClient. Never run integration tests.
- Never run ./gradlew test without --tests filter.
- Do NOT break existing tests.
- If fix requires a new UseCase — follow UseCase pattern.
- Minimal diff. Don't touch what isn't broken.