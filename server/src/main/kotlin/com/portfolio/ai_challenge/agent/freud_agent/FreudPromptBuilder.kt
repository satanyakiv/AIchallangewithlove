package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudAgentContext
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.MessageRole

class FreudPromptBuilder {

    fun buildStatePrompt(state: FreudSessionState, context: FreudAgentContext): String {
        val profile = context.userProfile
        return when (state) {
            is FreudSessionState.Begruessung -> Prompts.Freud.STATE_BEGRUESSUNG
            is FreudSessionState.FreeAssociation -> Prompts.Freud.STATE_FREE_ASSOCIATION
                .replace("{{turnCount}}", state.turnCount.toString())
                .replace("{{markers}}", buildMarkersSummary(profile))
            is FreudSessionState.Interpretation -> Prompts.Freud.STATE_INTERPRETATION
                .replace("{{defenseMechanisms}}", profile.defenseMechanisms.joinToString().ifBlank { "none identified yet" })
                .replace("{{fixationStage}}", profile.fixationStage ?: "not yet determined")
                .replace("{{childhoodThemes}}", profile.childhoodThemes.joinToString().ifBlank { "none detected" })
            is FreudSessionState.DreamAnalysis -> Prompts.Freud.STATE_DREAM_ANALYSIS
                .replace("{{dreamSymbols}}", profile.dreamSymbols.joinToString().ifBlank { "general dream content" })
            is FreudSessionState.Transference -> Prompts.Freud.STATE_TRANSFERENCE
            is FreudSessionState.Abschluss -> Prompts.Freud.STATE_ABSCHLUSS
        }
    }

    fun buildMessages(context: FreudAgentContext, state: FreudSessionState): List<DeepSeekMessage> {
        val systemPrompt = buildSystemWithProfile(context)
        val statePrompt = buildStatePrompt(state, context)
        return buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = systemPrompt))
            if (statePrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = statePrompt))
            }
            addAll(context.currentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
    }

    private fun buildSystemWithProfile(context: FreudAgentContext): String {
        val profile = context.userProfile
        return buildString {
            append(Prompts.Freud.SYSTEM)
            if (profile.patientName != null) append("\nThe patient's name is ${profile.patientName}.")
            if (profile.fixationStage != null) append("\nYou suspect ${profile.fixationStage} fixation.")
            if (profile.childhoodThemes.isNotEmpty()) {
                append("\nChildhood themes mentioned: ${profile.childhoodThemes.joinToString()}.")
            }
        }
    }

    private fun buildMarkersSummary(
        profile: com.portfolio.ai_challenge.agent.freud_agent.model.FreudUserProfile,
    ): String {
        val parts = mutableListOf<String>()
        if (profile.defenseMechanisms.isNotEmpty()) parts.add("defenses: ${profile.defenseMechanisms.joinToString()}")
        if (profile.childhoodThemes.isNotEmpty()) parts.add("childhood: ${profile.childhoodThemes.joinToString()}")
        if (profile.dreamSymbols.isNotEmpty()) parts.add("dreams: ${profile.dreamSymbols.joinToString()}")
        return parts.joinToString("; ").ifBlank { "none yet" }
    }
}
