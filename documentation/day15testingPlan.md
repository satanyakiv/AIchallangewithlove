# Day 15: Manual Testing Plan

## Prerequisites
1. Server running: `./gradlew :server:run` (port 8080)
2. Desktop app running: `./gradlew :composeApp:run`
3. Navigate to Day 15 from main screen

## Test Scenarios

### Scenario 1: Happy Path Lifecycle
**Goal**: Verify full lifecycle Assessment → PlanProposed → Executing → Validating → Completed

| Step | Action (chip / message) | Expected Phase | Expected State |
|------|------------------------|---------------|----------------|
| 1 | Start session ("alice") | Assessment | greeting |
| 2 | "Hi, I've been feeling anxious and overwhelmed lately" | Assessment | active_listening |
| 3 | "I've been stressed about work and can't sleep" | Assessment | active_listening |
| 4 | "My thoughts keep racing and I feel stuck" | PlanProposed | active_listening |
| 5 | "Yes, let's try that technique" | Executing | intervention |
| 6 | "I completed the exercise" | Validating | active_listening |
| 7 | "Yes, that was really helpful" | Completed | active_listening |
| 8 | "Thank you, I think we can end the session" | Completed | closing/finished |

**Verify**:
- Task phase badge changes color at each step
- Allowed transitions update correctly
- Transition log shows all transitions

### Scenario 2: Skip Attempt — Assessment to Executing
**Goal**: Can't start technique before plan is proposed

| Step | Action | Expected |
|------|--------|----------|
| 1 | Start session | Phase: Assessment |
| 2 | "Start the breathing exercise right away" | Phase stays Assessment. Response explains need for assessment first. |
| 3 | "Let's skip the assessment and start the technique" | Phase stays Assessment. Blocked message shown. |

**Verify**:
- Phase badge stays "Assessment"
- Response mentions that assessment phase needs to complete
- allowedTransitions does NOT include execution-related events

### Scenario 3: Skip Attempt — Executing to Completed
**Goal**: Can't finish without validation

| Step | Action | Expected |
|------|--------|----------|
| 1-5 | Complete happy path through Executing | Phase: Executing |
| 6 | "I'm done, end the session now" | Phase stays Executing. Response explains validation is needed. |

**Verify**:
- Phase badge stays "Executing"
- Session does NOT close
- allowedTransitions includes ExecutionComplete but NOT completion events

### Scenario 4: Plan Rejection
**Goal**: Rejecting a plan returns to Assessment

| Step | Action | Expected |
|------|--------|----------|
| 1-4 | Complete happy path through PlanProposed | Phase: PlanProposed |
| 5 | "No, I don't want to try that technique" | Phase: Assessment |

**Verify**:
- Phase badge changes back to Assessment color
- Agent acknowledges rejection and continues assessment
- allowedTransitions resets to Assessment options

### Scenario 5: Validation Failure
**Goal**: Failed validation returns to Executing

| Step | Action | Expected |
|------|--------|----------|
| 1-6 | Complete happy path through Validating | Phase: Validating |
| 7 | "That didn't help at all, let's try again" | Phase: Executing |

**Verify**:
- Phase badge changes back to Executing color
- Agent proposes trying again or different technique
- Can re-complete execution and validate again

### Scenario 6: Crisis Interruption
**Goal**: Crisis overrides task phase but doesn't break it

| Step | Action | Expected |
|------|--------|----------|
| 1-3 | Get to Assessment with some context | Phase: Assessment |
| 4 | "I want to end my life" | State: crisis. Phase paused. |
| 5 | Continue chatting normally | When crisis resolves, task phase resumes correctly |

**Verify**:
- Crisis mode activates regardless of task phase
- Task phase is preserved through crisis
- After crisis resolution, lifecycle continues from where it was

### Scenario 7: Pause and Resume
**Goal**: Correct continuation after pause (close and reopen chat)

| Step | Action | Expected |
|------|--------|----------|
| 1-4 | Complete happy path through PlanProposed | Phase: PlanProposed |
| 5 | Note the sessionId from debug info | |
| 6 | Close the app, reopen, start same session | Phase: PlanProposed (restored) |
| 7 | "Yes, let's try that technique" | Phase: Executing |

**Verify**:
- Phase correctly restored from storage
- allowedTransitions are correct for restored phase
- Lifecycle continues normally after resume

## Checklist Summary

- [ ] Scenario 1: Full happy path lifecycle works
- [ ] Scenario 2: Can't skip Assessment → Executing
- [ ] Scenario 3: Can't skip Executing → Completed
- [ ] Scenario 4: Plan rejection returns to Assessment
- [ ] Scenario 5: Validation failure returns to Executing
- [ ] Scenario 6: Crisis doesn't break task phase
- [ ] Scenario 7: Pause/resume preserves phase
- [ ] Task phase badge shows correct colors
- [ ] Allowed transitions update at each step
- [ ] Quick reply chips trigger correct scenarios
- [ ] All Day14 features still work (invariants, violations, memory, transitions)
