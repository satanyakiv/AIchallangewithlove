package com.portfolio.ai_challenge.agent.psy_agent.model

import com.portfolio.ai_challenge.agent.psy_agent.MemoryLayersDebug
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.StateTransition

data class PsyPipelineResult(
    val response: String,
    val state: String,
    val intent: SessionIntent,
    val transitions: List<StateTransition>,
    val profileUpdates: List<String>,
    val memoryLayers: MemoryLayersDebug,
)
