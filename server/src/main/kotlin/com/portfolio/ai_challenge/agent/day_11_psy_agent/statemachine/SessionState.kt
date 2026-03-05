package com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine

sealed interface SessionState {
    val displayName: String

    data object Greeting : SessionState {
        override val displayName = "greeting"
    }

    data class ActiveListening(
        val turnCount: Int = 0,
        val detectedEmotions: List<String> = emptyList(),
    ) : SessionState {
        override val displayName = "active_listening"
    }

    data class Intervention(
        val technique: String,
        val step: Int = 0,
        val totalSteps: Int = 3,
    ) : SessionState {
        override val displayName = "intervention"
    }

    data class CrisisMode(
        val riskLevel: String,
        val escalatedAt: Long,
    ) : SessionState {
        override val displayName = "crisis"
    }

    data class Closing(val summary: String? = null) : SessionState {
        override val displayName = "closing"
    }

    data object Finished : SessionState {
        override val displayName = "finished"
    }

    companion object {
        fun fromStorageString(s: String): SessionState = when {
            s == "greeting" -> Greeting
            s.startsWith("active_listening:") -> {
                val parts = s.split(":")
                ActiveListening(turnCount = parts.getOrNull(1)?.toIntOrNull() ?: 0)
            }
            s.startsWith("intervention:") -> {
                val parts = s.split(":")
                Intervention(
                    technique = parts.getOrNull(1) ?: "breathing",
                    step = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                    totalSteps = parts.getOrNull(3)?.toIntOrNull() ?: 3,
                )
            }
            s.startsWith("crisis:") -> {
                val parts = s.split(":")
                CrisisMode(
                    riskLevel = parts.getOrNull(1) ?: "high",
                    escalatedAt = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis(),
                )
            }
            s == "closing" -> Closing()
            s == "finished" -> Finished
            else -> Greeting
        }
    }
}

fun SessionState.toStorageString(): String = when (this) {
    is SessionState.Greeting -> "greeting"
    is SessionState.ActiveListening -> "active_listening:$turnCount"
    is SessionState.Intervention -> "intervention:$technique:$step:$totalSteps"
    is SessionState.CrisisMode -> "crisis:$riskLevel:$escalatedAt"
    is SessionState.Closing -> "closing"
    is SessionState.Finished -> "finished"
}
