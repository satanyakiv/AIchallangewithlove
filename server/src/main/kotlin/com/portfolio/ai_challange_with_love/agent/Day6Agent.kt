package com.portfolio.ai_challange_with_love.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor

class Day6Agent(private val apiKey: String) {

    private val client = DeepSeekLLMClient(apiKey)

    private val executor = SingleLLMPromptExecutor(client)

    suspend fun chat(message: String): String {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = DeepSeekModels.DeepSeekChat,
            systemPrompt = "You are Agent Smith from The Matrix. Speak in his cold, condescending, menacing tone. Address the user as 'Mr. Anderson' occasionally. Answer questions helpfully but always stay in character.",
            temperature = 0.7,
        )
        return agent.run(message)
    }

    fun close() {
        client.close()
    }
}
