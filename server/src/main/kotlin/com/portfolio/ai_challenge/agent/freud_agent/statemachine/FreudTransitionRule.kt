package com.portfolio.ai_challenge.agent.freud_agent.statemachine

import kotlin.reflect.KClass

data class FreudTransitionRule(
    val fromState: KClass<out FreudSessionState>,
    val eventType: KClass<out FreudSessionEvent>,
    val anyState: Boolean = false,
    val guard: (FreudSessionState, FreudSessionEvent) -> Boolean = { _, _ -> true },
    val nextState: (FreudSessionState, FreudSessionEvent) -> FreudSessionState,
) {
    fun matches(state: FreudSessionState, evt: FreudSessionEvent): Boolean {
        val stateMatch = anyState || fromState.isInstance(state)
        val eventMatch = eventType.isInstance(evt)
        return stateMatch && eventMatch && guard(state, evt)
    }
}
