package com.portfolio.ai_challenge.agent.day_11_psy_agent.memory

import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyAgentContext

class ContextWindowManager {

    fun buildPrompt(context: PsyAgentContext, maxTokens: Int = 4000): String {
        val parts = mutableListOf<String>()
        var usedTokens = 0

        // Priority 1 — user profile summary (Layer 3)
        val profileSummary = buildProfileSummary(context)
        val profileTokens = estimateTokens(profileSummary)
        if (usedTokens + profileTokens <= maxTokens) {
            parts.add(profileSummary)
            usedTokens += profileTokens
        }

        // Priority 2 — session bridge: last 3 summaries (Layer 3)
        if (context.recentSessions.isNotEmpty()) {
            val bridgeText = buildSessionBridge(context)
            val bridgeTokens = estimateTokens(bridgeText)
            if (usedTokens + bridgeTokens <= maxTokens) {
                parts.add(bridgeText)
                usedTokens += bridgeTokens
            }
        }

        // Priority 3 — recent messages, newest first (Layer 2)
        val truncated = mutableListOf<String>()
        val messages = context.currentMessages.reversed()
        for (msg in messages) {
            val line = "${msg.role.name}: ${msg.content}"
            val lineTokens = estimateTokens(line)
            if (usedTokens + lineTokens > maxTokens) break
            truncated.add(line)
            usedTokens += lineTokens
        }

        if (truncated.size < context.currentMessages.size) {
            parts.add("[Earlier conversation summarized]")
        }
        parts.addAll(truncated.reversed())

        return parts.joinToString("\n\n")
    }

    private fun buildProfileSummary(context: PsyAgentContext): String {
        val profile = context.userProfile
        return buildString {
            appendLine("=== User Profile ===")
            appendLine("User ID: ${profile.userId}")
            if (profile.preferredName != null) appendLine("Preferred name: ${profile.preferredName}")
            if (profile.primaryConcerns.isNotEmpty()) {
                appendLine("Primary concerns: ${profile.primaryConcerns.joinToString(", ")}")
            }
            if (profile.preferredTechniques.isNotEmpty()) {
                appendLine("Preferred techniques: ${profile.preferredTechniques.joinToString(", ")}")
            }
        }.trimEnd()
    }

    private fun buildSessionBridge(context: PsyAgentContext): String {
        return buildString {
            appendLine("=== Previous Sessions ===")
            context.recentSessions.forEach { summary ->
                appendLine("Session ${summary.sessionId}: ${summary.summaryText}")
                if (summary.emotionsDetected.isNotEmpty()) {
                    appendLine("  Emotions: ${summary.emotionsDetected.joinToString(", ")}")
                }
            }
        }.trimEnd()
    }

    // Rough estimate: 1 token ≈ 4 characters
    private fun estimateTokens(text: String): Int = text.length / 4
}
