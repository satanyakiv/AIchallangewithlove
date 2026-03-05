Read .claude/rules/architecture.md, .claude/rules/testing.md, .claude/rules/prompts.md

## Project context

We are building MindGuard — a mental health AI agent (psy-agent).
Architecture: Layered (Routes → Agent → UseCases → Store).
Agent has a pipeline: Plan → Execute → Validate → Done.
Key components: SessionStateMachine, InvariantChecker, ProfileExtractor,
PersonalizeResponseUseCase, DetectCrisisUseCase, ContextStore.
Memory: 3 layers (TurnContext in-memory, SessionContext persisted, UserProfile persistent).
LLM calls go through shared LlmClient. Prompts loaded from resources/prompts/psy/.
All code lives in server/.../agent/day_11_psy_agent/.

## Task

$ARGUMENTS

## Process

1. **READ**: Scan all files in server/.../agent/day_11_psy_agent/ and
   existing tests in server/src/test/. Understand what exists.

2. **PLAN**: Show me a checklist:
    - Files to create (with package path)
    - Files to modify (what changes)
    - New tests (list each test name and what it verifies)
    - New UI elements (if any)
      **Stop and wait for my "go" before writing code.**

3. **TEST FIRST**: Write all tests. They should fail. Run:
   `./gradlew :server:test --tests "*ClassName"` to confirm failures.

4. **IMPLEMENT**: Write production code. Follow UseCase pattern for
   any logic beyond simple orchestration. Prompts in .txt resources.

5. **VERIFY**: Run tests again. All must pass. Show summary:
    - Created: [files]
    - Modified: [files]
    - Tests: X passed / Y total

## Rules reminder
- Mock LlmClient in tests. Never call real API.
- Never run ./gradlew test without --tests filter.
- Every data mutation needs 3 tests: happy path, no-op, persistence.
- Files < 150 lines. Functions < 20 lines.
- Do NOT break existing tests.