package com.portfolio.ai_challenge.agent.psy_agent

data class ProfileUpdate(
    val preferredName: String?,
    val newConcerns: List<String>,
    val newTriggers: List<String>,
)

class ProfileExtractor {

    fun extract(message: String): ProfileUpdate {
        val lower = message.lowercase()
        return ProfileUpdate(
            preferredName = extractName(lower, message),
            newConcerns = extractConcerns(lower),
            newTriggers = extractTriggers(lower),
        )
    }

    private fun extractName(lower: String, original: String): String? {
        for (pattern in listOf("my name is ", "call me ", "i am ")) {
            val idx = lower.indexOf(pattern)
            if (idx >= 0) {
                val word = original.substring(idx + pattern.length).trim()
                    .split(" ").firstOrNull()?.trimEnd('.', ',', '!', '?') ?: continue
                return word.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    private fun extractConcerns(lower: String): List<String> = buildList {
        if ("anxious" in lower || "anxiety" in lower || "worried" in lower || "nervous" in lower)
            add("anxiety")
        if ("sleep" in lower || "insomnia" in lower || "cant sleep" in lower || "nightmares" in lower)
            add("sleep issues")
        if ("sad" in lower || "depressed" in lower || "hopeless" in lower || "empty" in lower)
            add("depression")
    }

    private fun extractTriggers(lower: String): List<String> = buildList {
        if ("work" in lower || "deadline" in lower || "boss" in lower || "job" in lower)
            add("work stress")
        if ("family" in lower || "parents" in lower || "partner" in lower || "relationship" in lower)
            add("family dynamics")
        if ("lonely" in lower || "alone" in lower || "isolated" in lower)
            add("loneliness")
    }
}
