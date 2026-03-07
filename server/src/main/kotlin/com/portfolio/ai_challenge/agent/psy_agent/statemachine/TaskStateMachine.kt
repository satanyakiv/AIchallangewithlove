package com.portfolio.ai_challenge.agent.psy_agent.statemachine

class TaskStateMachine(private val rules: List<TaskTransitionRule>) {

    var phase: TaskPhase = TaskPhase.Assessment
        private set

    private val _history = mutableListOf<StateTransition>()
    val history: List<StateTransition> get() = _history.toList()

    fun transition(event: TaskLifecycleEvent): Result<TaskPhase> {
        val rule = rules.firstOrNull { it.matches(phase, event) }
            ?: return Result.failure(
                IllegalStateException("No task transition from ${phase.displayName} on ${event.displayName}"),
            )
        val nextPhase = rule.nextPhase(phase, event)
        _history.add(
            StateTransition(
                from = phase.displayName,
                to = nextPhase.displayName,
                event = event.displayName,
            ),
        )
        phase = nextPhase
        return Result.success(nextPhase)
    }

    fun canTransition(event: TaskLifecycleEvent): Boolean =
        rules.any { it.matches(phase, event) }

    fun allowedEvents(): List<String> =
        TaskLifecycleEvent::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .filter { canTransition(it) }
            .map { it.displayName }

    fun restorePhase(restored: TaskPhase) {
        phase = restored
    }
}
