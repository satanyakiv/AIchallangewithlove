package com.portfolio.ai_challenge.agent.psy_agent

import com.portfolio.ai_challenge.agent.Prompts
import com.portfolio.ai_challenge.agent.psy_agent.memory.ContextStore
import com.portfolio.ai_challenge.agent.psy_agent.model.ConversationEntry
import com.portfolio.ai_challenge.agent.psy_agent.model.PsyChatResult
import com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext
import com.portfolio.ai_challenge.agent.psy_agent.model.SessionIntent
import com.portfolio.ai_challenge.agent.psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskLifecycleEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTaskTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.toStorageString
import com.portfolio.ai_challenge.models.LlmClient
import com.portfolio.ai_challenge.models.MessageRole

class Day15PsyAgent(
    private val contextStore: ContextStore,
    private val promptBuilder: PsyPromptBuilder,
    private val llmClient: LlmClient,
    private val updateProfile: UpdateProfileUseCase,
    private val detectCrisis: DetectCrisisUseCase,
    private val intentMapper: SessionStateToIntentMapper,
    private val validateAndRetry: ValidateAndRetryUseCase,
    private val enforcePhase: EnforceTaskPhaseUseCase,
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

        val machine = restoreMachine(session)
        val taskMachine = restoreTaskMachine(sessionId)

        val blocked = checkPhaseEnforcement(taskMachine, userMessage, machine)
        if (blocked != null) return blocked

        applyUserEvent(machine, userMessage)
        syncTaskPhase(taskMachine, machine, userMessage)

        val intent = intentMapper.map(machine.state)
        val attemptCount = session.messages.count { it.role == MessageRole.USER } + 1
        contextStore.appendMessage(sessionId, ConversationEntry(role = MessageRole.USER, content = userMessage))

        val profileUpdate = updateProfile.execute(session.userId, userMessage)
        val turnContext = buildTurnContext(sessionId, profileUpdate, intent, attemptCount)

        val validationResult = generateValidatedReply(sessionId, machine)
        contextStore.updateSessionState(sessionId, machine.state.toStorageString())
        contextStore.updateTaskPhase(sessionId, taskMachine.phase.toStorageString())

        return assembleResult(sessionId, machine, taskMachine, validationResult, intent, turnContext, profileUpdate)
    }

    fun getPhaseInfo(sessionId: String): Pair<String, List<String>> {
        val taskMachine = restoreTaskMachine(sessionId)
        return taskMachine.phase.displayName to taskMachine.allowedEvents()
    }

    private fun restoreMachine(session: PsySessionContext): SessionStateMachine {
        val machine = SessionStateMachine(mindGuardTransitions)
        machine.restoreState(SessionState.fromStorageString(session.currentState))
        return machine
    }

    private fun restoreTaskMachine(sessionId: String): TaskStateMachine {
        val machine = TaskStateMachine(mindGuardTaskTransitions)
        val stored = contextStore.loadTaskPhase(sessionId)
        if (stored != null) machine.restorePhase(TaskPhase.fromStorageString(stored))
        return machine
    }

    private fun checkPhaseEnforcement(
        taskMachine: TaskStateMachine,
        userMessage: String,
        sessionMachine: SessionStateMachine,
    ): PsyChatResult? {
        val sessionEvent = inferSessionEvent(userMessage, sessionMachine)
        if (sessionEvent != null) {
            val check = enforcePhase.execute(taskMachine.phase, sessionEvent)
            if (check is EnforceTaskPhaseUseCase.PhaseCheck.Blocked) {
                return buildBlockedResult(check, taskMachine)
            }
        }
        return null
    }

    private fun inferSessionEvent(userMessage: String, machine: SessionStateMachine): SessionEvent? {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("end the session") || lower.contains("goodbye") -> SessionEvent.SessionEndRequested
            machine.state is SessionState.Greeting && lower.contains("start the") -> SessionEvent.TechniqueProposed("", 3)
            else -> null
        }
    }

    private fun buildBlockedResult(
        check: EnforceTaskPhaseUseCase.PhaseCheck.Blocked,
        taskMachine: TaskStateMachine,
    ): PsyChatResult {
        val response = Prompts.Psy.TASK_BLOCKED
            .replace("{{phase}}", taskMachine.phase.displayName)
            .replace("{{required}}", check.requiredPhase)
            .replace("{{reason}}", check.reason)
        val emptySession = contextStore.loadSession("")
        val emptyProfile = contextStore.loadProfile("")
        return PsyChatResult(
            response = response,
            state = "blocked",
            session = emptySession ?: com.portfolio.ai_challenge.agent.psy_agent.model.PsySessionContext("", ""),
            profile = emptyProfile,
            turnContext = TurnContext(plan = "blocked"),
            intent = SessionIntent.Welcome,
            taskPhase = taskMachine.phase.displayName,
            allowedTransitions = taskMachine.allowedEvents(),
        )
    }

    private fun applyUserEvent(machine: SessionStateMachine, userMessage: String) {
        val crisisResult = detectCrisis.execute(userMessage)
        if (crisisResult.isPositive) {
            machine.transition(SessionEvent.CrisisDetected(crisisResult.level, crisisResult.indicators))
        } else {
            machine.transition(SessionEvent.UserMessage(content = userMessage, hasCrisisIndicators = false))
        }
    }

    private fun syncTaskPhase(taskMachine: TaskStateMachine, sessionMachine: SessionStateMachine, msg: String) {
        val lower = msg.lowercase()
        val phase = taskMachine.phase
        when {
            phase is TaskPhase.Assessment && shouldProposePlan(sessionMachine) ->
                taskMachine.transition(TaskLifecycleEvent.AssessmentComplete)
            phase is TaskPhase.PlanProposed && isApproval(lower) ->
                taskMachine.transition(TaskLifecycleEvent.PlanApproved)
            phase is TaskPhase.PlanProposed && isRejection(lower) ->
                taskMachine.transition(TaskLifecycleEvent.PlanRejected)
            phase is TaskPhase.Executing && isCompletionSignal(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ExecutionComplete)
            phase is TaskPhase.Validating && isPositiveValidation(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ValidationPassed)
            phase is TaskPhase.Validating && isNegativeValidation(lower) ->
                taskMachine.transition(TaskLifecycleEvent.ValidationFailed)
        }
    }

    private fun shouldProposePlan(machine: SessionStateMachine): Boolean {
        val state = machine.state
        return state is SessionState.ActiveListening && state.turnCount >= 3
    }

    private fun isApproval(msg: String): Boolean =
        msg.contains("yes") || msg.contains("let's try") || msg.contains("okay") || msg.contains("sure")

    private fun isRejection(msg: String): Boolean =
        msg.contains("no") && (msg.contains("don't want") || msg.contains("not"))

    private fun isCompletionSignal(msg: String): Boolean =
        msg.contains("completed") || msg.contains("done") || msg.contains("finished the exercise")

    private fun isPositiveValidation(msg: String): Boolean =
        msg.contains("helpful") || msg.contains("better") || msg.contains("thank")

    private fun isNegativeValidation(msg: String): Boolean =
        msg.contains("didn't help") || msg.contains("try again") || msg.contains("not helpful")

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
        taskMachine: TaskStateMachine,
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
            taskPhase = taskMachine.phase.displayName,
            allowedTransitions = taskMachine.allowedEvents(),
        )
    }
}
