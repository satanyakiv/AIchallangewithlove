package com.portfolio.ai_challenge.agent.freud_agent.statemachine

sealed interface FreudSessionState {
    val displayName: String

    data object Begruessung : FreudSessionState {
        override val displayName = "begruessung"
    }

    data class FreeAssociation(val turnCount: Int = 0) : FreudSessionState {
        override val displayName = "free_association"
    }

    data class Interpretation(val topic: String = "general") : FreudSessionState {
        override val displayName = "interpretation"
    }

    data class DreamAnalysis(val dreamCount: Int = 1) : FreudSessionState {
        override val displayName = "dream_analysis"
    }

    data object Transference : FreudSessionState {
        override val displayName = "transference"
    }

    data object Abschluss : FreudSessionState {
        override val displayName = "abschluss"
    }

    companion object {
        fun fromStorageString(s: String): FreudSessionState = when {
            s == "begruessung" -> Begruessung
            s.startsWith("free_association:") -> {
                val turnCount = s.substringAfter(":").toIntOrNull() ?: 0
                FreeAssociation(turnCount = turnCount)
            }
            s.startsWith("interpretation:") -> {
                val topic = s.substringAfter(":").ifBlank { "general" }
                Interpretation(topic = topic)
            }
            s.startsWith("dream_analysis:") -> {
                val dreamCount = s.substringAfter(":").toIntOrNull() ?: 1
                DreamAnalysis(dreamCount = dreamCount)
            }
            s == "transference" -> Transference
            s == "abschluss" -> Abschluss
            else -> Begruessung
        }
    }
}

fun FreudSessionState.toStorageString(): String = when (this) {
    is FreudSessionState.Begruessung -> "begruessung"
    is FreudSessionState.FreeAssociation -> "free_association:$turnCount"
    is FreudSessionState.Interpretation -> "interpretation:$topic"
    is FreudSessionState.DreamAnalysis -> "dream_analysis:$dreamCount"
    is FreudSessionState.Transference -> "transference"
    is FreudSessionState.Abschluss -> "abschluss"
}
