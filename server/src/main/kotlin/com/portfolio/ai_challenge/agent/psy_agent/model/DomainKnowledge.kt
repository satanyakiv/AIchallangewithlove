package com.portfolio.ai_challenge.agent.psy_agent.model

data class Technique(
    val name: String,
    val description: String,
    val stepsCount: Int,
    val suitableFor: List<String>,
)

data class DomainKnowledge(val techniques: List<Technique>)

val DEFAULT_DOMAIN = DomainKnowledge(
    listOf(
        Technique(
            name = "Progressive Muscle Relaxation",
            description = "Systematically tense and release muscle groups to reduce physical tension and anxiety",
            stepsCount = 3,
            suitableFor = listOf("anxiety", "stress"),
        ),
        Technique(
            name = "Box Breathing",
            description = "Inhale for 4 counts, hold for 4, exhale for 4, hold for 4 — regulates the nervous system",
            stepsCount = 4,
            suitableFor = listOf("anxiety", "panic"),
        ),
        Technique(
            name = "Cognitive Restructuring",
            description = "Identify and challenge negative automatic thoughts to replace them with balanced perspectives",
            stepsCount = 5,
            suitableFor = listOf("depression", "negative-thinking"),
        ),
    )
)
