package com.portfolio.ai_challenge.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor

class Day6Agent(
    private val client: DeepSeekLLMClient,
    private val executor: MultiLLMPromptExecutor,
) {

    suspend fun chat(message: String): String {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = DeepSeekModels.DeepSeekChat,
            systemPrompt = Prompts.Day6.SYSTEM,
            temperature = 0.7,
        )
        return agent.run(message)
    }

    fun close() {
        client.close()
    }
}
