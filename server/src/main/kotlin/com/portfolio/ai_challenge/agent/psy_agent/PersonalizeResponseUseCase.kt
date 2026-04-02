package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.psy_agent.model.Formality
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionSummary
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyUserProfile
import com.portfolio.ai_challenge.agent.psy_agent.model.ResponseLength
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class PersonalizeResponseUseCase {

    fun buildPersonalizedSystemPrompt(context: PsyAgentContext): String {
        val prefs = context.userProfile.preferences
        val formalityInstruction = when (prefs.formality) {
            Formality.FORMAL -> Prompts.Psy.PERSONALIZATION_FORMAL
            Formality.INFORMAL -> Prompts.Psy.PERSONALIZATION_INFORMAL
            Formality.MIXED -> Prompts.Psy.PERSONALIZATION_MIXED
        }
        val lengthInstruction = when (prefs.responseLength) {
            ResponseLength.SHORT -> Prompts.Psy.PERSONALIZATION_SHORT
            ResponseLength.MEDIUM -> Prompts.Psy.PERSONALIZATION_MEDIUM
            ResponseLength.DETAILED -> Prompts.Psy.PERSONALIZATION_DETAILED
        }
        val avoidSection = if (prefs.avoidTopics.isNotEmpty())
            "\nNever discuss or bring up these topics: ${prefs.avoidTopics.joinToString()}."
        else ""
        return buildString {
            append(Prompts.Psy.SYSTEM)
            append("\n\n").append(formalityInstruction)
            append("\n").append(lengthInstruction)
            append("\n\n").append(buildUserSummary(context.userProfile))
            val bridge = buildSessionBridge(context.recentSessions)
            if (bridge.isNotEmpty()) append("\n\n").append(bridge)
            append(avoidSection)
            append("\nRespond in ${prefs.language}.")
        }
    }

    fun buildUserSummary(profile: PsyUserProfile): String {
        val hasData = profile.preferredName != null || profile.primaryConcerns.isNotEmpty()
            || profile.knownTriggers.isNotEmpty()
        if (!hasData) return "New client. No prior information available. Start by getting to know them."
        return "Client: ${profile.preferredName ?: "unknown"}. " +
            "Known concerns: ${profile.primaryConcerns.joinToString()}. " +
            "Known triggers: ${profile.knownTriggers.joinToString()}. " +
            "Preferred techniques: ${profile.preferredTechniques.joinToString()}. " +
            "Sessions completed: ${profile.sessionHistory.size}."
    }

    fun buildSessionBridge(recentSessions: List<PsySessionSummary>): String {
        if (recentSessions.isEmpty()) return ""
        return recentSessions.takeLast(3).joinToString("\n") { s ->
            val date = Instant.fromEpochMilliseconds(s.timestampMs)
                .toLocalDateTime(TimeZone.UTC).date
            "Previous session ($date): discussed ${s.topicsDiscussed.joinToString()}, " +
                "used ${s.techniquesUsed.joinToString()}. Homework: ${s.homework}."
        }
    }
}
