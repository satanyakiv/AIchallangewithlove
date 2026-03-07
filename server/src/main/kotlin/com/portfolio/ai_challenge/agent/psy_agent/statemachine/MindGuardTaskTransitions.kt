package com.portfolio.ai_challenge.agent.psy_agent.statemachine

val mindGuardTaskTransitions: List<TaskTransitionRule> = listOf(
    // Assessment -> PlanProposed
    TaskTransitionRule(
        fromPhase = TaskPhase.Assessment::class,
        event = TaskLifecycleEvent.AssessmentComplete::class,
        nextPhase = { _, _ -> TaskPhase.PlanProposed() },
    ),

    // PlanProposed -> Executing (approved)
    TaskTransitionRule(
        fromPhase = TaskPhase.PlanProposed::class,
        event = TaskLifecycleEvent.PlanApproved::class,
        nextPhase = { phase, _ ->
            val plan = (phase as TaskPhase.PlanProposed).plan
            TaskPhase.Executing(plan = plan)
        },
    ),

    // PlanProposed -> Assessment (rejected)
    TaskTransitionRule(
        fromPhase = TaskPhase.PlanProposed::class,
        event = TaskLifecycleEvent.PlanRejected::class,
        nextPhase = { _, _ -> TaskPhase.Assessment },
    ),

    // Executing -> Validating
    TaskTransitionRule(
        fromPhase = TaskPhase.Executing::class,
        event = TaskLifecycleEvent.ExecutionComplete::class,
        nextPhase = { _, _ -> TaskPhase.Validating },
    ),

    // Validating -> Completed
    TaskTransitionRule(
        fromPhase = TaskPhase.Validating::class,
        event = TaskLifecycleEvent.ValidationPassed::class,
        nextPhase = { _, _ -> TaskPhase.Completed },
    ),

    // Validating -> Executing (failed)
    TaskTransitionRule(
        fromPhase = TaskPhase.Validating::class,
        event = TaskLifecycleEvent.ValidationFailed::class,
        nextPhase = { _, _ -> TaskPhase.Executing() },
    ),
)
