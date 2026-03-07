package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyUserProfile
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.StateTransition
import kotlinx.serialization.Serializable

@Serializable
data class MemoryLayersDebug(
    val turn: String,
    val session: String,
    val profile: String,
)

@Serializable
data class PsyChatResponse(
    val response: String,
    val state: String,
    val memoryLayers: MemoryLayersDebug,
    val profileUpdates: List<String> = emptyList(),
    val intent: String = "",
    val transitions: List<TransitionDebug> = emptyList(),
    val violations: List<String> = emptyList(),
    val taskPhase: String = "",
    val allowedTransitions: List<String> = emptyList(),
)

@Serializable
data class TransitionDebug(val from: String, val to: String, val event: String)

class PsyResponseMapper {

    fun buildMemoryDebug(
        session: PsySessionContext,
        profile: PsyUserProfile,
        turn: TurnContext,
    ): MemoryLayersDebug = MemoryLayersDebug(
        turn = "{ plan: ${turn.plan}, attemptCount: ${turn.attemptCount}, detectedEmotion: ${turn.detectedEmotion} }",
        session = "{ messageCount: ${session.messages.size}, detectedEmotions: ${session.detectedEmotions}, currentState: ${session.currentState} }",
        profile = "{ userId: ${profile.userId}, preferredName: ${profile.preferredName}, concerns: ${profile.primaryConcerns}, formality: ${profile.preferences.formality} }",
    )

    fun toChatResponse(result: PsyChatResult): PsyChatResponse = PsyChatResponse(
        response = result.response,
        state = result.state,
        memoryLayers = buildMemoryDebug(result.session, result.profile, result.turnContext),
        profileUpdates = result.profileUpdates,
        intent = result.intent.apiName,
        transitions = result.transitions.map { TransitionDebug(it.from, it.to, it.event) },
        violations = result.violations,
        taskPhase = result.taskPhase,
        allowedTransitions = result.allowedTransitions,
    )

    fun toTransitionDebug(t: StateTransition): TransitionDebug =
        TransitionDebug(from = t.from, to = t.to, event = t.event)
}
