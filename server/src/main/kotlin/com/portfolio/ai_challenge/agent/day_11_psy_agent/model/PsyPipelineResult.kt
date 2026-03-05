package com.portfolio.ai_challenge.agent.day_11_psy_agent.model

import com.portfolio.ai_challenge.agent.day_11_psy_agent.MemoryLayersDebug
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.StateTransition

data class PsyPipelineResult(
    val response: String,
    val state: String,
    val intent: String,
    val transitions: List<StateTransition>,
    val profileUpdates: List<String>,
    val memoryLayers: MemoryLayersDebug,
)
