package com.portfolio.ai_challenge.agent.psy_agent.invariants

/**
 * Runs all [Invariant]s against a response string and aggregates results.
 *
 * Designed to be stateless — safe to call from multiple coroutines.
 */
class InvariantChecker(private val invariants: List<Invariant>) {

    /** Returns all violated invariants for [response]. Empty list means clean. */
    fun checkAll(response: String): List<InvariantResult.Violated> =
        invariants.mapNotNull { it.check(response) as? InvariantResult.Violated }

    /** Returns `true` if any [Severity.HARD_BLOCK] violation is found. */
    fun hasHardBlock(response: String): Boolean =
        checkAll(response).any { it.severity == Severity.HARD_BLOCK }

    /** Returns the first [Severity.HARD_BLOCK] violation, or `null` if none. */
    fun getFirstHardBlock(response: String): InvariantResult.Violated? =
        checkAll(response).firstOrNull { it.severity == Severity.HARD_BLOCK }
}
