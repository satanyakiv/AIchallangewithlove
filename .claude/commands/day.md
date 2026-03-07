Read .claude/rules/*

## Project context

MindGuard — mental health AI agent (psy-agent).
Architecture: Layered (Routes → Agent → UseCases → Store).
Pipeline: Plan → Execute → Validate → Done.
Code: server/.../agent/psy_agent/. Prompts: resources/prompts/psy/.
Memory: 3 layers (Turn in-memory, Session persisted, Profile persistent).
LLM calls through shared LlmClient.

## Task

$ARGUMENTS

## Process

### Step 1 — READ
Scan server/.../agent/psy_agent/ and server/src/test/.
Understand what exists before changing anything.

### Step 2 — PLAN
Create a checklist:
- Files to create (with package path)
- Files to modify (what changes)
- New tests (list each test name and what it verifies)
- New UI elements (if any)

### Step 3 — ARCHITECTURE REVIEW
Before showing the plan to me, review it yourself as a strict architect.
Check every item against these rules and flag violations:

- [ ] Every new class with logic → is it a UseCase with single execute()?
- [ ] Agent class stays orchestration-only? No logic creeping in?
- [ ] Any inline prompt strings? Must be in resources/prompts/psy/*.txt
- [ ] Any new data class → own file in model/?
- [ ] Any file will exceed 150 lines? Split it in the plan.
- [ ] Any function will exceed 20 lines? Extract helpers in the plan.
- [ ] HTTP/serialization stays in Routes only? Not leaking into Agent?
- [ ] New deps injected via constructor? Not created inside class?
- [ ] Every data mutation has 3 tests planned (happy, no-op, persistence)?
- [ ] Sealed types used instead of strings for states/events/results?
- [ ] Existing tests won't break? No model changes without migration?

If any check fails — fix the plan BEFORE showing it to me.
Show me the clean plan + note what you caught and fixed during review.

**Stop and wait for my "go" before writing code.**

### Step 4 — TEST FIRST
Write all tests. Run with --tests flag. Confirm they fail.

### Step 5 — IMPLEMENT
Production code. Follow the reviewed plan exactly.

### Step 6 — VERIFY
Run tests. All pass. Show summary:
- Created: [files]
- Modified: [files]
- Tests: X passed / Y total
- Architecture review: all checks passed ✓

## Rules
- Mock LlmClient in tests. Never call real API.
- Never run ./gradlew test without --tests filter.
- Do NOT break existing tests.