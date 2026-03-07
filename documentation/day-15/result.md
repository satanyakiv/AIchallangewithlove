# Day 15: Controlled Task State Transitions

## Idea

A therapeutic AI agent (MindGuard) guides users through structured psychological exercises. Without controlled transitions, the agent could skip critical phases — e.g., jumping from assessment directly to execution without proposing a plan, or marking a task complete without validation.

**Goal**: Implement a finite state machine (FSM) that enforces a strict task lifecycle, preventing phase skipping and ensuring every therapeutic technique follows the full Assessment → Plan → Execute → Validate → Complete flow.

## Architecture

Dual-layer FSM design:

- **SessionStateMachine** — controls conversation flow (Greeting → ActiveListening → Intervention → Closing → Finished). Existed since Day 11.
- **TaskStateMachine** — controls task lifecycle within a session (Assessment → PlanProposed → Executing → Validating → Completed). Added in Day 15.

`EnforceTaskPhaseUseCase` bridges the two layers by blocking session-level events that are incompatible with the current task phase.

**Diagrams**: see [class diagram](../day-11-to-15/psy-agent-class-diagram.md), [state diagram](../day-11-to-15/psy-agent-state-diagram.md), [session state machine](../day-11-to-15/session-state-machine.md).

## Transition Rules

7 transitions defined in `MindGuardTaskTransitions`:

| # | From         | Event              | To           |
|---|--------------|--------------------|--------------|
| 1 | Assessment   | AssessmentComplete | PlanProposed |
| 2 | PlanProposed | PlanApproved       | Executing    |
| 3 | PlanProposed | PlanRejected       | Assessment   |
| 4 | Executing    | ExecutionComplete  | Validating   |
| 5 | Validating   | ValidationPassed   | Completed    |
| 6 | Validating   | ValidationFailed   | Executing    |

**Blocked transitions** (no rules exist, FSM returns `Result.failure`):

- Assessment → Executing (must go through PlanProposed first)
- Assessment → Completed (must pass all intermediate phases)
- Executing → Completed (must go through Validating first)
- PlanProposed → Validating (must go through Executing first)

## Phase Enforcement Constraints

`EnforceTaskPhaseUseCase` maps session events to minimum required task phases:

| SessionEvent        | Assessment | PlanProposed | Executing | Validating | Completed |
|---------------------|------------|--------------|-----------|------------|-----------|
| UserMessage         | Allowed    | Allowed      | Allowed   | Allowed    | Allowed   |
| TechniqueProposed   | Blocked    | Allowed      | Allowed   | Allowed    | Allowed   |
| TechniqueAccepted   | Blocked    | Blocked      | Allowed   | Allowed    | Allowed   |
| SessionEndRequested | Blocked    | Blocked      | Blocked   | Allowed    | Allowed   |

When blocked, returns `PhaseCheck.Blocked(reason, requiredPhase)` — the agent uses this to explain why the action isn't available yet.

## Key Components

| File                          | Responsibility                                                                                 |
|-------------------------------|------------------------------------------------------------------------------------------------|
| `TaskPhase.kt`                | Sealed interface: 5 phases with string serialization                                           |
| `TaskStateMachine.kt`         | FSM core: `transition()`, `canTransition()`, `allowedEvents()`, history tracking, pause/resume |
| `MindGuardTaskTransitions.kt` | 7 transition rules (from → event → to)                                                         |
| `EnforceTaskPhaseUseCase.kt`  | Blocks session events incompatible with current task phase                                     |
| `Day15PsyAgent.kt`            | Orchestrates both FSMs, routes messages through enforcement layer                              |

## Testing

### Unit Tests — 28 tests in 6 categories

| Category        | Count | What is verified                                                                                       |
|-----------------|-------|--------------------------------------------------------------------------------------------------------|
| Transitions     | 8     | Each valid transition, full lifecycle, initial state, history recording                                |
| Skip Prevention | 4     | Assessment→Executing, Assessment→Completed, Executing→Completed, PlanProposed→Validating — all blocked |
| Enforcement     | 5     | TechniqueProposed/TechniqueAccepted/SessionEnd blocked/allowed per phase, UserMessage always allowed   |
| Serialization   | 5     | Roundtrip for each phase (serialize → deserialize → equal)                                             |
| Pause/Resume    | 2     | Phase preserved after pause, transitions work after resume                                             |
| Persistence     | 4     | ContextStore save/load task phase, overwrite, unknown session fallback                                 |

### Manual Testing — 7 scenarios

1. **Happy Path** — full lifecycle Assessment → Completed
2. **Skip Assessment→Executing** — blocked, agent explains plan needed
3. **Skip Executing→Completed** — blocked, validation required
4. **Plan Rejection** — PlanProposed → back to Assessment
5. **Validation Failure** — Validating → back to Executing
6. **Crisis Interruption** — crisis overrides phase, resumes after
7. **Pause/Resume** — close app mid-session, phase restored on reopen

Details: see [testing plan](../day15testingPlan.md).

## Possible Improvements

- **Timeout-based transitions** — auto-advance if user is idle in a phase too long (e.g., gentle nudge after 5 min in Assessment)
- **Weighted transition costs** — some transitions could require more "evidence" before being allowed (e.g., multiple validation signals before Completed)
- **Phase-specific prompts** — dynamic system prompt sections injected based on current TaskPhase, giving the LLM phase-aware instructions
- **Parallel task tracks** — support multiple concurrent tasks in different phases within one session
- **Transition analytics** — log transition patterns across sessions to identify where users commonly get stuck or drop off
