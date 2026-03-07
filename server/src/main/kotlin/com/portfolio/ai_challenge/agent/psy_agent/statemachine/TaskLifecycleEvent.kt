package com.portfolio.ai_challenge.agent.psy_agent.statemachine

sealed interface TaskLifecycleEvent {
    val displayName: String

    data object AssessmentComplete : TaskLifecycleEvent {
        override val displayName = "assessment_complete"
    }

    data object PlanApproved : TaskLifecycleEvent {
        override val displayName = "plan_approved"
    }

    data object PlanRejected : TaskLifecycleEvent {
        override val displayName = "plan_rejected"
    }

    data object ExecutionComplete : TaskLifecycleEvent {
        override val displayName = "execution_complete"
    }

    data object ValidationPassed : TaskLifecycleEvent {
        override val displayName = "validation_passed"
    }

    data object ValidationFailed : TaskLifecycleEvent {
        override val displayName = "validation_failed"
    }
}
