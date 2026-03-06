package com.portfolio.ai_challenge.agent.psy_agent.statemachine

data class StateTransition(
    val from: String,
    val to: String,
    val event: String,
    val timestamp: Long = System.currentTimeMillis(),
)
