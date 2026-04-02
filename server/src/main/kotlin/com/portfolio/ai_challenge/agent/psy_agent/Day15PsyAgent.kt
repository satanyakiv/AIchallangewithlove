package com.portfolio.ai_challenge.agent.psy_agent

import io.github.oshai.kotlinlogging.KotlinLogging
import com.portfolio.ai_challenge.models.getOrThrow
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTaskTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.toStorageString
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

private val logger = KotlinLogging.logger {}

class Day15PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
    private val detectCrisis: DetectCrisisUseCase,
    private val intentMapper: SessionStateToIntentMapper,
    private val validateAndRetry: ValidateAndRetryUseCase,
    private val syncPhase: SyncTaskPhaseUseCase,
    private val resultAssembler: Day15ResultAssembler,
) {

    fun startSession(userId: String): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        contextStore.createSession(sessionId, userId)
        contextStore.updateTaskPhase(sessionId, TaskPhase.Assessment.toStorageString())
        return sessionId
    }

    suspend fun chat(sessionId: String, userMessage: String): PsyChatResult {
        val session = contextStore.loadSession(sessionId)
            ?: throw IllegalArgumentException("Session not found: $sessionId")
        logger.debug { "Chat: session=$sessionId, state=${session.currentState}" }
        val machine = restoreMachine(session.currentState)
        val taskMachine = restoreTaskMachine(sessionId)

        val blocked = resultAssembler.checkPhaseEnforcement(taskMachine, userMessage, machine)
        if (blocked != null) return blocked

        applyUserEvent(machine, userMessage)
        syncPhase.execute(taskMachine, machine, userMessage)

        val intent = intentMapper.map(machine.state)
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val turnContext = resultAssembler.buildTurnContext(sessionId, profileUpdate, intent, attemptCount)
        val validation = generateValidatedReply(sessionId, machine)

        contextStore.updateSessionState(sessionId, machine.state.toStorageString())
        contextStore.updateTaskPhase(sessionId, taskMachine.phase.toStorageString())

        return resultAssembler.assembleResult(sessionId, machine, taskMachine, validation, intent, turnContext, profileUpdate)
    }

    fun getPhaseInfo(sessionId: String): Pair<String, List<String>> {
        val taskMachine = restoreTaskMachine(sessionId)
        return taskMachine.phase.displayName to taskMachine.allowedEvents()
    }

    private fun restoreMachine(stateString: String) =
        SessionStateMachine(mindGuardTransitions).also { it.restoreState(SessionState.fromStorageString(stateString)) }

    private fun restoreTaskMachine(sessionId: String) =
        TaskStateMachine(mindGuardTaskTransitions).also { m ->
            contextStore.loadTaskPhase(sessionId)?.let { m.restorePhase(TaskPhase.fromStorageString(it)) }
        }

    private fun applyUserEvent(machine: SessionStateMachine, userMessage: String) {
        val crisisResult = detectCrisis.execute(userMessage)
        if (crisisResult.isPositive) {
            machine.transition(SessionEvent.CrisisDetected(crisisResult.level, crisisResult.indicators))
        } else {
            machine.transition(SessionEvent.UserMessage(content = userMessage, hasCrisisIndicators = false))
        }
    }

    private suspend fun generateValidatedReply(
        sessionId: String,
        machine: SessionStateMachine,
    ): ValidateAndRetryUseCase.ValidationResult {
        val context = contextStore.assembleContext(sessionId, machine.state.displayName)
        val messages = promptBuilder.buildMessages(context, machine.state)
        val initialResponse = llmClient.complete(messages, maxTokens = 300).getOrThrow()
        val result = validateAndRetry.execute(initialResponse, messages)
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.ASSISTANT, content = result.response))
        return result
    }
}
