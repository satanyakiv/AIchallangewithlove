package com.portfolio.ai_challenge.agent.psy_agent.invariants

interface Invariant {
    val name: String
    val rationale: String
    fun check(response: String): InvariantResult
    fun toPromptInstruction(): String
}
