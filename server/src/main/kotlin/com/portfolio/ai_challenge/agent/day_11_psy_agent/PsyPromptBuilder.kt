package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextWindowManager
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyAgentContext
import com.portfolio.ai_challenge.models.DeepSeekMessage
import com.portfolio.ai_challenge.models.MessageRole

private const val SYSTEM_PROMPT = """You are MindGuard, a compassionate and evidence-based mental health support AI.
Your role is to:
- Listen actively and validate the user's emotions
- Suggest appropriate evidence-based techniques (e.g., Box Breathing, Cognitive Restructuring, Progressive Muscle Relaxation)
- Be warm, non-judgmental, and professional
- Remind users that you are an AI and encourage seeking professional help for serious concerns

Always respond with empathy and care.

Response length rules:
- Maximum 3-4 sentences per response
- Never use numbered lists unless guiding a specific technique step by step
- Never repeat what the user just said back to them in full
- One idea per response. Ask one question at most.
- If suggesting a technique, give only the next single step, not all steps at once"""

class PsyPromptBuilder(private val contextWindowManager: ContextWindowManager) {

    fun buildMessages(context: PsyAgentContext): List<DeepSeekMessage> {
        val contextPrompt = contextWindowManager.buildPrompt(context)
        return buildList {
            add(DeepSeekMessage(role = MessageRole.SYSTEM, content = SYSTEM_PROMPT))
            if (contextPrompt.isNotBlank()) {
                add(DeepSeekMessage(role = MessageRole.SYSTEM, content = "Context:\n$contextPrompt"))
            }
            addAll(context.currentMessages.map { DeepSeekMessage(role = it.role, content = it.content) })
        }
    }
}
