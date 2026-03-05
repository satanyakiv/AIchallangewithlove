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
    }

    private fun load(path: String): String =
        Prompts::class.java.classLoader.getResource(path)!!.readText().trim()
}
