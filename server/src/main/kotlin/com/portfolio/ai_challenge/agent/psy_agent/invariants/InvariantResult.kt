package com.portfolio.ai_challenge.agent.psy_agent.invariants

sealed interface InvariantResult {
    data object Passed : InvariantResult

    data class Violated(
        val invariantName: String,
        val message: String,
        val severity: Severity,
    ) : InvariantResult
}
