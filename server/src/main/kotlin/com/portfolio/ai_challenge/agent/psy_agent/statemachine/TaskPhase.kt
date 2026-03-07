package com.portfolio.ai_challenge.agent.psy_agent.statemachine

sealed interface TaskPhase {
    val displayName: String

    data object Assessment : TaskPhase {
        override val displayName = "assessment"
    }

    data class PlanProposed(val plan: String = "") : TaskPhase {
        override val displayName = "plan_proposed"
    }

    data class Executing(val plan: String = "") : TaskPhase {
        override val displayName = "executing"
    }

    data object Validating : TaskPhase {
        override val displayName = "validating"
    }

    data object Completed : TaskPhase {
        override val displayName = "completed"
    }

    companion object {
        fun fromStorageString(s: String): TaskPhase = when {
            s == "assessment" -> Assessment
            s.startsWith("plan_proposed:") -> PlanProposed(plan = s.removePrefix("plan_proposed:"))
            s.startsWith("executing:") -> Executing(plan = s.removePrefix("executing:"))
            s == "validating" -> Validating
            s == "completed" -> Completed
            else -> Assessment
        }
    }
}

fun TaskPhase.toStorageString(): String = when (this) {
    is TaskPhase.Assessment -> "assessment"
    is TaskPhase.PlanProposed -> "plan_proposed:$plan"
    is TaskPhase.Executing -> "executing:$plan"
    is TaskPhase.Validating -> "validating"
    is TaskPhase.Completed -> "completed"
}
