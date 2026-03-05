package com.portfolio.ai_challenge.agent.day_11_psy_agent

import com.portfolio.ai_challenge.agent.day_11_psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.mindGuardTransitions
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.toStorageString
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

/**
 * Day 13 — Psy-Agent with a formal typed [SessionStateMachine].
 *
 * Route prefix: `/api/agent/psy13/`
 *
 * Extends Day 12 with:
 * - **Typed session states** — [SessionState] sealed interface (Greeting → ActiveListening →
 *   Intervention → Closing → Finished) with guarded [TransitionRule]s.
 * - **Crisis detection pipeline** — [DetectCrisisUseCase] runs first; a positive result
 *   fires [SessionEvent.CrisisDetected] instead of [SessionEvent.UserMessage], bypassing
 *   normal lifecycle transitions.
 * - **State persistence** — state string (e.g. `"active_listening:3"`) is stored in
 *   [ContextStore] after each turn and restored via [SessionStateMachine.restoreState].
 * - **Intent & transitions** — [PsyChatResult] carries [intent] and [transitions] for
 *   the UI state badge and transition log.
 */
class Day13PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
    private val detectCrisis: DetectCrisisUseCase,
    private val determineIntent: DetermineIntentUseCase,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")

        val machine = restoreMachine(session)
        applyUserEvent(machine, userMessage)

        val intent = determineIntent.execute(machine.state, TurnContext())
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val turnContext = buildTurnContext(sessionId, profileUpdate, intent, attemptCount)

        val response = generateReply(sessionId, machine)
        contextStore.updateSessionState(sessionId, machine.state.toStorageString())

        return assembleResult(sessionId, machine, response, intent, turnContext, profileUpdate)
    }

    private fun restoreMachine(session: PsySessionContext): SessionStateMachine {
        val machine = SessionStateMachine(mindGuardTransitions)
        machine.restoreState(SessionState.fromStorageString(session.currentState))
        return machine
    }

    private fun applyUserEvent(machine: SessionStateMachine, userMessage: String) {
        val crisisResult = detectCrisis.execute(userMessage)
        if (crisisResult.isPositive) {
            machine.transition(SessionEvent.CrisisDetected(crisisResult.level, crisisResult.indicators))
        } else {
            machine.transition(SessionEvent.UserMessage(content = userMessage, hasCrisisIndicators = false))
        }
    }

    private fun buildTurnContext(
        sessionId: String,
        profileUpdate: ProfileUpdate,
        intent: String,
        attemptCount: Int,
    ): TurnContext {
        val detectedEmotion = profileUpdate.newConcerns.firstOrNull()
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }
        val plan = if (detectedEmotion != null) "validate_and_explore" else intent
        return TurnContext(attemptCount = attemptCount, detectedEmotion = detectedEmotion, plan = plan)
    }

    private suspend fun generateReply(sessionId: String, machine: SessionStateMachine): String {
        val context = contextStore.assembleContext(sessionId, machine.state.displayName)
        val messages = promptBuilder.buildMessages(context, machine.state)
        val response = llmClient.complete(messages, maxTokens = 300)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = response))
        return response
    }

    private fun assembleResult(
        sessionId: String,
        machine: SessionStateMachine,
        response: String,
        intent: String,
        turnContext: TurnContext,
        profileUpdate: ProfileUpdate,
    ): PsyChatResult {
        val updatedSession = contextStore.loadSession(sessionId)!!
        val profile = contextStore.loadProfile(updatedSession.userId)
        val profileUpdateStrings = buildList {
            profileUpdate.preferredName?.let { add("name: $it") }
            profileUpdate.newConcerns.forEach { add("concern: $it") }
            profileUpdate.newTriggers.forEach { add("trigger: $it") }
        }
        return PsyChatResult(
            response = response,
            state = machine.state.displayName,
            session = updatedSession,
            profile = profile,
            turnContext = turnContext,
            profileUpdates = profileUpdateStrings,
            intent = intent,
            transitions = machine.history,
        )
    }
}
