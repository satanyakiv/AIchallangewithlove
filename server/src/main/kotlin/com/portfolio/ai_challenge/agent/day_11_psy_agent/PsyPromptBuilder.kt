package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.MessageRole

class PsyPromptBuilder(private val contextWindowManager: ContextWindowManager) {

    fun buildMessages(context: PsyAgentContext): List<DeepSeekMessage> {
        val contextPrompt = contextWindowManager.buildPrompt(context)
        return buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = Prompts.Psy.SYSTEM))
            if (contextPrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = "Context:\n$contextPrompt"))
            }
            addAll(context.currentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
    }
}
