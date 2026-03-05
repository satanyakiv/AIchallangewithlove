package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore

class UpdateProfileUseCase(
    private val profileExtractor: ProfileExtractor,
    private val contextStore: ContextStore,
) {

    fun execute(userId: String, message: String): ProfileUpdate {
        val update = profileExtractor.extract(message)
        if (update.preferredName != null || update.newConcerns.isNotEmpty() || update.newTriggers.isNotEmpty()) {
            val profile = contextStore.loadProfile(userId)
            contextStore.saveProfile(
                profile.copy(
                    preferredName = update.preferredName ?: profile.preferredName,
                    primaryConcerns = (profile.primaryConcerns + update.newConcerns).distinct(),
                    knownTriggers = (profile.knownTriggers + update.newTriggers).distinct(),
                )
            )
        }
        return update
    }
}
