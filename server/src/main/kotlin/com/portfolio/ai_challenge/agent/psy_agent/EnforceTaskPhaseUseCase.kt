package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase

class EnforceTaskPhaseUseCase {

    sealed interface PhaseCheck {
        data object Allowed : PhaseCheck
        data class Blocked(val reason: String, val requiredPhase: String) : PhaseCheck
    }

    fun execute(taskPhase: TaskPhase, sessionEvent: SessionEvent): PhaseCheck {
        return when (sessionEvent) {
            is SessionEvent.TechniqueProposed -> requireAtLeast(taskPhase, "plan_proposed")
            is SessionEvent.TechniqueAccepted -> requireAtLeast(taskPhase, "executing")
            is SessionEvent.SessionEndRequested -> requireAtLeast(taskPhase, "validating")
            else -> PhaseCheck.Allowed
        }
    }

    private fun requireAtLeast(current: TaskPhase, required: String): PhaseCheck {
        val order = phaseOrder(current.displayName)
        val requiredOrder = phaseOrder(required)
        return if (order >= requiredOrder) {
            PhaseCheck.Allowed
        } else {
            PhaseCheck.Blocked(
                reason = "Task is in '${current.displayName}' phase. " +
                    "Required phase: '$required' or later.",
                requiredPhase = required,
            )
        }
    }

    private fun phaseOrder(name: String): Int = when (name) {
        "assessment" -> 0
        "plan_proposed" -> 1
        "executing" -> 2
        "validating" -> 3
        "completed" -> 4
        else -> -1
    }
}
