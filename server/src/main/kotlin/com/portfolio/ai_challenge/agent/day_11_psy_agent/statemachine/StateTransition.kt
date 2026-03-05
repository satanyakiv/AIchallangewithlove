package com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine

data class StateTransition(
    val from: String,
    val to: String,
    val event: String,
    val timestamp: Long = System.currentTimeMillis(),
)
