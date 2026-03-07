package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.SessionIntent
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.toStorageString
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

/**
 * Day 14 — Psy-Agent with invariant validation and retry pipeline.
 *
 * Route prefix: `/api/agent/psy14/`
 *
 * Extends Day 13 with a **VALIDATE** phase:
 * - After LLM responds, [ValidateAndRetryUseCase] checks all [invariants].
 * - [Severity.HARD_BLOCK] → retry with injected constraint instructions (max 3).
 * - All retries exhausted → safe fallback response.
 * - [Severity.SOFT_FIX] violations → logged in [PsyChatResult.violations].
 */
class Day14PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
    private val detectCrisis: DetectCrisisUseCase,
    private val intentMapper: SessionStateToIntentMapper,
    private val validateAndRetry: ValidateAndRetryUseCase,
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

        val intent = intentMapper.map(machine.state)
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val turnContext = buildTurnContext(sessionId, profileUpdate, intent, attemptCount)

        val validationResult = generateValidatedReply(sessionId, machine)
        contextStore.updateSessionState(sessionId, machine.state.toStorageString())

        return assembleResult(sessionId, machine, validationResult, intent, turnContext, profileUpdate)
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
        intent: SessionIntent,
        attemptCount: Int,
    ): TurnContext {
        val detectedEmotion = profileUpdate.newConcerns.firstOrNull()
        if (profileUpdate.newConcerns.isNotEmpty()) {
            contextStore.updateSessionEmotions(sessionId, profileUpdate.newConcerns)
        }
        val plan = if (detectedEmotion != null) "validate_and_explore" else intent.apiName
        return TurnContext(attemptCount = attemptCount, detectedEmotion = detectedEmotion, plan = plan)
    }

    private suspend fun generateValidatedReply(
        sessionId: String,
        machine: SessionStateMachine,
    ): ValidateAndRetryUseCase.ValidationResult {
        val context = contextStore.assembleContext(sessionId, machine.state.displayName)
        val messages = promptBuilder.buildMessages(context, machine.state)
        val initialResponse = llmClient.complete(messages, maxTokens = 300)
        val result = validateAndRetry.execute(initialResponse, messages)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = result.response))
        return result
    }

    private fun assembleResult(
        sessionId: String,
        machine: SessionStateMachine,
        validation: ValidateAndRetryUseCase.ValidationResult,
        intent: SessionIntent,
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
            response = validation.response,
            state = machine.state.displayName,
            session = updatedSession,
            profile = profile,
            turnContext = turnContext,
            profileUpdates = profileUpdateStrings,
            intent = intent,
            transitions = machine.history,
            violations = validation.violations,
        )
    }
}
