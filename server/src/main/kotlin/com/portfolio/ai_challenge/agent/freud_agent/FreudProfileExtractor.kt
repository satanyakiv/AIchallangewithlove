package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.model.FreudProfileUpdate

class FreudProfileExtractor {

    fun extract(message: String): FreudProfileUpdate {
        val lower = message.lowercase()
        return FreudProfileUpdate(
            patientName = extractName(lower, message),
            newDreamSymbols = extractDreamSymbols(lower),
            newDefenseMechanisms = extractDefenseMechanisms(lower),
            newChildhoodThemes = extractChildhoodThemes(lower),
            detectedFixation = detectFixation(lower),
            newRelationshipPatterns = extractRelationshipPatterns(lower),
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

    private fun extractDreamSymbols(lower: String): List<String> = buildList {
        val dreamKeywords = listOf("dream", "dreamt", "nightmare", "sleep", "vision")
        if (dreamKeywords.any { it in lower }) add("dream_content")
    }

    private fun extractDefenseMechanisms(lower: String): List<String> = buildList {
        if ("deny" in lower || "denial" in lower) add("denial")
        if ("blame" in lower || "projection" in lower) add("projection")
        if ("angry" in lower || "anger" in lower) add("displacement")
        if ("avoid" in lower || "avoidance" in lower) add("avoidance")
        if ("joke" in lower || "humor" in lower) add("humor")
    }

    private fun extractChildhoodThemes(lower: String): List<String> = buildList {
        if ("mother" in lower || "mom" in lower || "mama" in lower) add("mother")
        if ("father" in lower || "dad" in lower || "papa" in lower) add("father")
        if ("brother" in lower || "sister" in lower || "sibling" in lower) add("sibling")
        if ("childhood" in lower || "child" in lower || "kid" in lower) add("childhood")
    }

    private fun detectFixation(lower: String): String? = when {
        listOf("food", "eating", "coffee", "smoking").any { it in lower } -> "oral"
        listOf("organized", "clean", "control", "perfectionist").any { it in lower } -> "anal"
        listOf("attention", "show-off", "competitive").any { it in lower } -> "phallic"
        else -> null
    }

    private fun extractRelationshipPatterns(lower: String): List<String> = buildList {
        if ("boss" in lower || "authority" in lower || "supervisor" in lower) add("authority_figure")
        if ("partner" in lower || "spouse" in lower || "dating" in lower) add("romantic_attachment")
    }
}
