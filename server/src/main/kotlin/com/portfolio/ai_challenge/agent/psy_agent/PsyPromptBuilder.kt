package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.MessageRole

class PsyPromptBuilder(
    private val contextWindowManager: ContextWindowManager,
    private val personalizeUseCase: PersonalizeResponseUseCase,
) {

    fun buildStatePrompt(state: SessionState): String = when (state) {
        is SessionState.Greeting -> Prompts.Psy.STATE_GREETING
        is SessionState.ActiveListening -> Prompts.Psy.STATE_ACTIVE_LISTENING
            .replace("{{turnCount}}", state.turnCount.toString())
            .replace("{{emotions}}", state.detectedEmotions.joinToString().ifBlank { "none detected" })
        is SessionState.Intervention -> Prompts.Psy.STATE_INTERVENTION
            .replace("{{step}}", state.step.toString())
            .replace("{{totalSteps}}", state.totalSteps.toString())
            .replace("{{technique}}", state.technique)
        is SessionState.CrisisMode -> Prompts.Psy.STATE_CRISIS
        is SessionState.Closing -> Prompts.Psy.STATE_CLOSING
        is SessionState.Finished -> ""
    }

    fun buildMessages(context: PsyAgentContext, state: SessionState): List<DeepSeekMessage> {
        val systemPrompt = personalizeUseCase.buildPersonalizedSystemPrompt(context)
        val statePrompt = buildStatePrompt(state)
        val contextPrompt = contextWindowManager.buildPrompt(context)
        return buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = systemPrompt))
            if (statePrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = statePrompt))
            }
            if (contextPrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = "Context:\n$contextPrompt"))
            }
            addAll(context.currentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
    }

    // Backward-compat overload for Day11 agent
    fun buildMessages(context: PsyAgentContext): List<DeepSeekMessage> =
        buildMessages(context, SessionState.ActiveListening())
}
