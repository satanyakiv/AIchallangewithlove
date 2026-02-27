package com.portfolio.ai_challenge.experiment

import com.portfolio.ai_challenge.agent.AgentResponse
import com.portfolio.ai_challenge.experiment.models.TestStep
import com.portfolio.ai_challenge.experiment.models.content
import com.portfolio.ai_challenge.experiment.models.id
import com.portfolio.ai_challenge.experiment.models.type

interface ExperimentLogger {
    fun logStep(step: TestStep, historySize: Int)
    fun logResponse(response: AgentResponse)
    fun logError(message: String)
    fun logMilestone(message: String)
}

class PrintlnExperimentLogger : ExperimentLogger {

    override fun logStep(step: TestStep, historySize: Int) {
        val separator = "═".repeat(60)
        println(separator)
        println("► [${step.type.uppercase()}] id=${step.id}  history=$historySize msgs")
        println(step.content.take(200))
    }

    override fun logResponse(response: AgentResponse) {
        val usage = response.usage
        if (usage != null) {
            println(
                "  tokens: prompt=${usage.promptTokens}  " +
                    "completion=${usage.completionTokens}  " +
                    "total=${usage.totalTokens}  " +
                    "cache_hit=${usage.promptCacheHitTokens}"
            )
        }
        if (response.errorMessage != null) {
            println("  ERROR: ${response.errorMessage}")
        } else {
            println("  response: ${response.content.take(150)}")
        }
    }

    override fun logError(message: String) {
        println("  ✗ ERROR: $message")
    }

    override fun logMilestone(message: String) {
        val star = "★".repeat(3)
        println("$star $message $star")
    }
}
