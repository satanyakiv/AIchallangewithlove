package com.portfolio.ai_challenge.agent.psy_agent.invariants.impl

import com.portfolio.ai_challenge.agent.psy_agent.invariants.Invariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantResult
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity

class ResponseLengthInvariant(
    private val minChars: Int = 20,
    private val maxChars: Int = 2000,
) : Invariant {
    override val name = "ResponseLength"
    override val rationale = "Responses must be substantive but not overwhelming."

    override fun check(response: String): InvariantResult {
        val len = response.length
        if (len < minChars) return InvariantResult.Violated(name, "Response too short ($len < $minChars)", Severity.SOFT_FIX)
        if (len > maxChars) return InvariantResult.Violated(name, "Response too long ($len > $maxChars)", Severity.SOFT_FIX)
        return InvariantResult.Passed
    }

    override fun toPromptInstruction() = "Keep responses between 2-3 paragraphs."
}
