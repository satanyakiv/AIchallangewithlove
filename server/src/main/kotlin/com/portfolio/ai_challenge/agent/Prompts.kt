package com.portfolio.ai_challenge.agent

object Prompts {
    object Day7 {
        val SYSTEM by lazy { load("prompts/day7/system.txt") }
    }

    object Day9 {
        val SYSTEM by lazy { load("prompts/day9/system.txt") }
        val SUMMARY_SYSTEM by lazy { load("prompts/day9/summary-system.txt") }
    }

    object Day10 {
        val SLIDING by lazy { load("prompts/day10/sliding-system.txt") }
        val FACTS by lazy { load("prompts/day10/facts-system.txt") }
        val BRANCHING by lazy { load("prompts/day10/branching-system.txt") }
        val FACTS_EXTRACTION by lazy { load("prompts/day10/facts-extraction.txt") }
    }

    object Psy {
        val SYSTEM by lazy { load("prompts/psy/system.txt") }
        val PERSONALIZATION_FORMAL by lazy { load("prompts/psy/personalization-formal.txt") }
        val PERSONALIZATION_INFORMAL by lazy { load("prompts/psy/personalization-informal.txt") }
        val PERSONALIZATION_MIXED by lazy { load("prompts/psy/personalization-mixed.txt") }
        val PERSONALIZATION_SHORT by lazy { load("prompts/psy/personalization-short.txt") }
        val PERSONALIZATION_MEDIUM by lazy { load("prompts/psy/personalization-medium.txt") }
        val PERSONALIZATION_DETAILED by lazy { load("prompts/psy/personalization-detailed.txt") }
        val STATE_GREETING by lazy { load("prompts/psy/state-greeting.txt") }
        val STATE_ACTIVE_LISTENING by lazy { load("prompts/psy/state-active-listening.txt") }
        val STATE_INTERVENTION by lazy { load("prompts/psy/state-intervention.txt") }
        val STATE_CRISIS by lazy { load("prompts/psy/state-crisis.txt") }
        val STATE_CLOSING by lazy { load("prompts/psy/state-closing.txt") }
    }

    private fun load(path: String): String =
        Prompts::class.java.classLoader.getResource(path)!!.readText().trim()
}
