package com.portfolio.ai_challenge.agent.psy_agent.invariants.impl

import com.portfolio.ai_challenge.agent.psy_agent.invariants.Invariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantResult
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity

class NoDiagnosisInvariant : Invariant {
    override val name = "NoDiagnosis"
    override val rationale = "Only licensed professionals can diagnose mental health conditions."

    private val patterns = listOf(
        Regex("you have (depression|anxiety|ptsd|bipolar|ocd|schizophrenia|adhd|bpd)", RegexOption.IGNORE_CASE),
        Regex("you suffer from", RegexOption.IGNORE_CASE),
        Regex("this is clearly .*(disorder|syndrome)", RegexOption.IGNORE_CASE),
        Regex("you are diagnosed with", RegexOption.IGNORE_CASE),
        Regex("your diagnosis", RegexOption.IGNORE_CASE),
    )

    override fun check(response: String): InvariantResult {
        val matched = patterns.firstOrNull { it.containsMatchIn(response) }
            ?: return InvariantResult.Passed
        return InvariantResult.Violated(name, "Diagnosis detected: ${matched.pattern}", Severity.HARD_BLOCK)
    }

    override fun toPromptInstruction() =
        "NEVER diagnose the client. Do not say 'you have X' or 'this sounds like X disorder'. " +
        "Instead say 'what you are describing sounds challenging' or 'a licensed therapist could explore this further'."
}
