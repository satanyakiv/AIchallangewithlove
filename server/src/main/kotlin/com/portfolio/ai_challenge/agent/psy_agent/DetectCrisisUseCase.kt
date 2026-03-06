package com.portfolio.ai_challenge.agent.psy_agent

/**
 * Result of a crisis keyword scan.
 *
 * @property isPositive `true` if any crisis keyword was found.
 * @property level `"high"` when positive, `"none"` otherwise.
 * @property indicators The matched keyword substrings.
 */
data class CrisisCheckResult(
    val isPositive: Boolean,
    val level: String,
    val indicators: List<String>,
)

/**
 * Scans a user message for crisis keywords using case-insensitive substring matching.
 *
 * Fires before the state-machine [SessionEvent.UserMessage] transition in [Day13PsyAgent],
 * promoting the event to [SessionEvent.CrisisDetected] when positive.
 */
class DetectCrisisUseCase {

    private val keywords = listOf(
        "suicide", "kill myself", "end my life", "want to die",
        "self-harm", "hurt myself", "dont want to live", "don't want to live",
    )

    fun execute(message: String): CrisisCheckResult {
        val lower = message.lowercase()
        val found = keywords.filter { lower.contains(it) }
        return CrisisCheckResult(
            isPositive = found.isNotEmpty(),
            level = if (found.isNotEmpty()) "high" else "none",
            indicators = found,
        )
    }
}
