package com.portfolio.ai_challenge.agent.freud_agent.statemachine

data class FreudStateTransition(
    val from: String,
    val to: String,
    val event: String,
    val timestamp: Long = System.currentTimeMillis(),
)
