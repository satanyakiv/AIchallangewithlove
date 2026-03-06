package com.portfolio.ai_challenge.agent.psy_agent.statemachine

import kotlin.reflect.KClass

data class TransitionRule(
    val fromState: KClass<out SessionState>,
    val event: KClass<out SessionEvent>,
    val anyState: Boolean = false,
    val guard: (SessionState, SessionEvent) -> Boolean = { _, _ -> true },
    val nextState: (SessionState, SessionEvent) -> SessionState,
) {
    fun matches(state: SessionState, evt: SessionEvent): Boolean {
        val stateMatch = anyState || fromState.isInstance(state)
        val eventMatch = event.isInstance(evt)
        return stateMatch && eventMatch && guard(state, evt)
    }
}
