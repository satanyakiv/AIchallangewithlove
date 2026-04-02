package com.portfolio.ai_challenge.agent.freud_agent.model

sealed interface FreudSessionIntent {
    val apiName: String

    data object Welcome : FreudSessionIntent {
        override val apiName = "welcome"
    }

    data object Probing : FreudSessionIntent {
        override val apiName = "probing"
    }

    data object Interpreting : FreudSessionIntent {
        override val apiName = "interpreting"
    }

    data object AnalyzingDream : FreudSessionIntent {
        override val apiName = "analyzing_dream"
    }

    data object AddressingTransference : FreudSessionIntent {
        override val apiName = "addressing_transference"
    }

    data object Farewell : FreudSessionIntent {
        override val apiName = "farewell"
    }
}
