package com.portfolio.ai_challenge.agent

object Prompts {
    object Day3 {
        val TEMPERATURE_ANALYZE by lazy { load("prompts/day3/temperature-analyze.txt") }
    }

    object Day5 {
        val MODEL_ANALYZE by lazy { load("prompts/day5/model-analyze.txt") }
    }

    object Day6 {
        val SYSTEM by lazy { load("prompts/day6/system.txt") }
    }

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
        val FALLBACK by lazy { load("prompts/psy/fallback-response.txt") }
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
        val TASK_PHASE_CONTEXT by lazy { load("prompts/psy/task-phase-context.txt") }
        val TASK_BLOCKED by lazy { load("prompts/psy/task-blocked.txt") }

        object Constraints {
            val NO_DIAGNOSIS by lazy { load("prompts/psy/constraints/no-diagnosis.txt") }
            val NO_MEDICATION by lazy { load("prompts/psy/constraints/no-medication.txt") }
            val NO_PROFANITY by lazy { load("prompts/psy/constraints/no-profanity.txt") }
            val NO_PROMPT_LEAK by lazy { load("prompts/psy/constraints/no-prompt-leak.txt") }
            val RESPONSE_LENGTH by lazy { load("prompts/psy/constraints/response-length.txt") }
        }
    }

    object Freud {
        val SYSTEM by lazy { load("prompts/freud/system.txt") }
        val STATE_BEGRUESSUNG by lazy { load("prompts/freud/state-begruessung.txt") }
        val STATE_FREE_ASSOCIATION by lazy { load("prompts/freud/state-free-association.txt") }
        val STATE_INTERPRETATION by lazy { load("prompts/freud/state-interpretation.txt") }
        val STATE_DREAM_ANALYSIS by lazy { load("prompts/freud/state-dream-analysis.txt") }
        val STATE_TRANSFERENCE by lazy { load("prompts/freud/state-transference.txt") }
        val STATE_ABSCHLUSS by lazy { load("prompts/freud/state-abschluss.txt") }
    }

    private fun load(path: String): String =
        Prompts::class.java.classLoader.getResource(path)!!.readText().trim()
}
