package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudContextStore
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudProfileUpdate

class UpdateFreudProfileUseCase(
    private val profileExtractor: FreudProfileExtractor,
    private val contextStore: FreudContextStore,
) {

    fun execute(userId: String, message: String): FreudProfileUpdate {
        val update = profileExtractor.extract(message)
        if (hasUpdates(update)) {
            val profile = contextStore.loadProfile(userId)
            contextStore.saveProfile(
                profile.copy(
                    patientName = update.patientName ?: profile.patientName,
                    defenseMechanisms = (profile.defenseMechanisms + update.newDefenseMechanisms).distinct(),
                    childhoodThemes = (profile.childhoodThemes + update.newChildhoodThemes).distinct(),
                    dreamSymbols = (profile.dreamSymbols + update.newDreamSymbols).distinct(),
                    fixationStage = update.detectedFixation ?: profile.fixationStage,
                    relationshipPatterns = (profile.relationshipPatterns + update.newRelationshipPatterns).distinct(),
                ),
            )
        }
        return update
    }

    private fun hasUpdates(update: FreudProfileUpdate): Boolean =
        update.patientName != null ||
            update.newDreamSymbols.isNotEmpty() ||
            update.newDefenseMechanisms.isNotEmpty() ||
            update.newChildhoodThemes.isNotEmpty() ||
            update.detectedFixation != null ||
            update.newRelationshipPatterns.isNotEmpty()
}
