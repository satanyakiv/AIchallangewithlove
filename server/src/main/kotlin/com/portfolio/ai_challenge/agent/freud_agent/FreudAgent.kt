package com.portfolio.ai_challenge.agent.freud_agent

import com.portfolio.ai_challenge.agent.freud_agent.memory.FreudContextStore
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudChatResult
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudProfileUpdate
import com.portfolio.ai_challenge.agent.freud_agent.model.FreudTurnContext
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudStateMachine
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.freudTransitions
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.toStorageString
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

class FreudAgent(
    private val contextStore: FreudContextStore,
    private val promptBuilder: FreudPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateFreudProfileUseCase,
    private val intentMapper: FreudStateToIntentMapper,
    private val detectEvent: DetectFreudEventUseCase,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): FreudChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        val machine = restoreMachine(session.currentState)
        val event = detectEvent.execute(userMessage)
        machine.transition(event)

        val intent = intentMapper.map(machine.state)
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val turnContext = FreudTurnContext(
            attemptCount = attemptCount,
            detectedMarker = profileUpdate.newDreamSymbols.firstOrNull() ?: profileUpdate.detectedFixation,
            plan = intent.apiName,
        )

        val response = generateReply(sessionId, machine)
        contextStore.updateSessionState(sessionId, machine.state.toStorageString())

        return assembleResult(sessionId, machine, response, intent, turnContext, profileUpdate)
    }

    private fun restoreMachine(currentState: String): FreudStateMachine {
        val machine = FreudStateMachine(freudTransitions)
        machine.restoreState(FreudSessionState.fromStorageString(currentState))
        return machine
    }

    private suspend fun generateReply(sessionId: String, machine: FreudStateMachine): String {
        val context = contextStore.assembleContext(sessionId, machine.state.displayName)
        val messages = promptBuilder.buildMessages(context, machine.state)
        val response = llmClient.complete(messages, maxTokens = 300)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))
        return response
    }

    private fun assembleResult(
        sessionId: String,
        machine: FreudStateMachine,
        response: String,
        intent: com.portfolio.ai_challenge.agent.freud_agent.model.FreudSessionIntent,
        turnContext: FreudTurnContext,
        profileUpdate: FreudProfileUpdate,
    ): FreudChatResult {
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(updatedSession.userId)
        return FreudChatResult(
            response = response,
            state = machine.state.displayName,
            session = updatedSession,
            profile = profile,
            turnContext = turnContext,
            profileUpdate = profileUpdate,
            intent = intent,
            transitions = machine.history,
        )
    }
}
