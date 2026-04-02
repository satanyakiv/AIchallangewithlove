package com.portfolio.ai_challenge.agent.freud_agent.statemachine

class FreudStateMachine(private val rules: List<FreudTransitionRule>) {

    var state: FreudSessionState = FreudSessionState.Begruessung
        private set

    private val _history = mutableListOf<FreudStateTransition>()
    val history: List<FreudStateTransition> get() = _history.toList()

    fun transition(event: FreudSessionEvent): Result<FreudSessionState> {
        val rule = rules.firstOrNull { it.matches(state, event) }
            ?: return Result.failure(
                IllegalStateException("No transition from ${state.displayName} on ${event::class.simpleName}"),
            )
        val nextState = rule.nextState(state, event)
        _history.add(
            FreudStateTransition(
                from = state.displayName,
                to = nextState.displayName,
                event = event::class.simpleName ?: "Unknown",
            ),
        )
        state = nextState
        return Result.success(nextState)
    }

    fun restoreState(restoredState: FreudSessionState) {
        state = restoredState
    }

    fun reset() {
        state = FreudSessionState.Begruessung
        _history.clear()
    }
}
