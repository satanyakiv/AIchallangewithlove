package com.portfolio.ai_challenge.agent.freud_agent.statemachine

sealed interface FreudSessionEvent {
    data class PatientMessage(val content: String) : FreudSessionEvent
    data object DreamDetected : FreudSessionEvent
    data object ResistanceDetected : FreudSessionEvent
    data object SessionEndRequested : FreudSessionEvent
}
