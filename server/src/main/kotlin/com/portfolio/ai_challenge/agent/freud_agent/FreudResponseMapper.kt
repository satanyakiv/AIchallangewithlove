package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.model.FreudChatResult
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudProfileUpdate
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudTurnContext
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudUserProfile
import com.portfolio.ai_challenge.agent.psy_agent.MemoryLayersDebug
import com.portfolio.ai_challenge.agent.psy_agent.PsyChatResponse
import com.portfolio.ai_challenge.agent.psy_agent.TransitionDebug
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext

class FreudResponseMapper {

    fun toChatResponse(result: FreudChatResult): PsyChatResponse = PsyChatResponse(
        response = result.response,
        state = result.state,
        memoryLayers = buildMemoryDebug(result.session, result.profile, result.turnContext),
        profileUpdates = buildProfileUpdateStrings(result.profileUpdate),
        intent = result.intent.apiName,
        transitions = result.transitions.map { TransitionDebug(it.from, it.to, it.event) },
    )

    private fun buildProfileUpdateStrings(update: FreudProfileUpdate): List<String> = buildList {
        update.patientName?.let { add("name: $it") }
        update.newDefenseMechanisms.forEach { add("defense: $it") }
        update.newChildhoodThemes.forEach { add("childhood: $it") }
        update.newDreamSymbols.forEach { add("dream: $it") }
        update.detectedFixation?.let { add("fixation: $it") }
        update.newRelationshipPatterns.forEach { add("relationship: $it") }
    }

    private fun buildMemoryDebug(
        session: PsySessionContext,
        profile: FreudUserProfile,
        turn: FreudTurnContext,
    ): MemoryLayersDebug = MemoryLayersDebug(
        turn = "{ plan: ${turn.plan}, attemptCount: ${turn.attemptCount}, marker: ${turn.detectedMarker} }",
        session = "{ messageCount: ${session.messages.size}, state: ${session.currentState} }",
        profile = "{ userId: ${profile.userId}, name: ${profile.patientName}, fixation: ${profile.fixationStage}, defenses: ${profile.defenseMechanisms} }",
    )
}
