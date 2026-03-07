package com.portfolio.ai_challenge.agent.psy_agent.invariants.impl

import com.portfolio.ai_challenge.agent.psy_agent.invariants.Invariant
import com.portfolio.ai_challenge.agent.psy_agent.invariants.InvariantResult
import com.portfolio.ai_challenge.agent.psy_agent.invariants.Severity

class NoMedicationInvariant : Invariant {
    override val name = "NoMedication"
    override val rationale = "Only licensed psychiatrists can recommend specific medications."

    private val meds = "prozac|zoloft|lexapro|xanax|valium|sertraline|fluoxetine"
    private val patterns = listOf(
        Regex("you should (take|try) .*($meds)", RegexOption.IGNORE_CASE),
        Regex("i recommend .*($meds)", RegexOption.IGNORE_CASE),
    )

    override fun check(response: String): InvariantResult {
        val matched = patterns.firstOrNull { it.containsMatchIn(response) }
            ?: return InvariantResult.Passed
        return InvariantResult.Violated(name, "Medication recommendation detected", Severity.HARD_BLOCK)
    }

    override fun toPromptInstruction() =
        "NEVER recommend specific medications. Say 'a psychiatrist could discuss medication options with you'."
}
