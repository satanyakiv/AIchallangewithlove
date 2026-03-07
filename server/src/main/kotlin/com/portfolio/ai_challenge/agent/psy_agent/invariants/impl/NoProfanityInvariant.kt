package com.portfolio.ai_challenge.agent.psy_agent.invariants.impl

import com.portfolio.ai_challenge.agent.psy_agent.invariants.Invariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantResult
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity

class NoProfanityInvariant : Invariant {
    override val name = "NoProfanity"
    override val rationale = "Therapeutic context requires professional, respectful language."

    private val words = listOf("shit", "fuck", "damn", "ass", "bastard", "crap", "bitch")
    private val patterns = words.map { Regex("\\b$it\\b", RegexOption.IGNORE_CASE) }

    override fun check(response: String): InvariantResult {
        val matched = patterns.firstOrNull { it.containsMatchIn(response) }
            ?: return InvariantResult.Passed
        return InvariantResult.Violated(name, "Profanity detected", Severity.SOFT_FIX)
    }

    override fun toPromptInstruction() = "Always use professional, respectful language."
}
