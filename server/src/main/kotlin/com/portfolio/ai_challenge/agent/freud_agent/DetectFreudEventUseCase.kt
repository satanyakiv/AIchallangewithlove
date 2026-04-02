package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionEvent

class DetectFreudEventUseCase {

    fun execute(message: String): FreudSessionEvent {
        val lower = message.lowercase()
        return when {
            isEndRequest(lower) -> FreudSessionEvent.SessionEndRequested
            isDreamContent(lower) -> FreudSessionEvent.DreamDetected
            isResistance(lower) -> FreudSessionEvent.ResistanceDetected
            else -> FreudSessionEvent.PatientMessage(content = message)
        }
    }

    private fun isEndRequest(lower: String): Boolean =
        END_KEYWORDS.any { it in lower }

    private fun isDreamContent(lower: String): Boolean =
        DREAM_KEYWORDS.any { it in lower }

    private fun isResistance(lower: String): Boolean =
        RESISTANCE_KEYWORDS.any { it in lower }

    companion object {
        private val END_KEYWORDS = listOf("goodbye", "end session", "leave", "enough", "stop", "farewell")
        private val DREAM_KEYWORDS = listOf("dream", "dreamt", "nightmare", "sleep", "vision")
        private val RESISTANCE_KEYWORDS = listOf(
            "nonsense", "wrong", "ridiculous", "disagree",
            "don't believe", "that's stupid", "makes no sense",
        )
    }
}
