package com.portfolio.ai_challenge.agent.psy_agent.statemachine

import kotlin.reflect.KClass

data class TaskTransitionRule(
    val fromPhase: KClass<out TaskPhase>,
    val event: KClass<out TaskLifecycleEvent>,
    val nextPhase: (TaskPhase, TaskLifecycleEvent) -> TaskPhase,
) {
    fun matches(phase: TaskPhase, evt: TaskLifecycleEvent): Boolean =
        fromPhase.isInstance(phase) && event.isInstance(evt)
}
