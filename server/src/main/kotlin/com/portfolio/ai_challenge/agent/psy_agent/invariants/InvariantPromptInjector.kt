package com.portfolio.ai_challenge.agent.psy_agent.invariants

/**
 * Appends invariant constraint instructions to a system prompt so the LLM
 * is explicitly reminded of what it must never do on a retry attempt.
 */
class InvariantPromptInjector(private val invariants: List<Invariant>) {

    fun inject(basePrompt: String): String = buildString {
        append(basePrompt)
        append("\n\n## Response Constraints\n")
        invariants.forEach { append("- ").append(it.toPromptInstruction()).append("\n") }
    }
}
