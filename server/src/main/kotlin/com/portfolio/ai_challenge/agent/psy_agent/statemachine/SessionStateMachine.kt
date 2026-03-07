package com.portfolio.ai_challenge.agent.psy_agent.statemachine

/**
 * Finite-state machine that governs a MindGuard therapy session.
 *
 * States and allowed transitions are defined by [rules] (see [mindGuardTransitions]).
 * State is serialised to a string for cross-request persistence via [toStorageString] /
 * [SessionState.Companion.fromStorageString] and stored in [ContextStore.updateSessionState].
 *
 * @param rules Ordered list of [TransitionRule]s. First matching rule wins.
 */
class SessionStateMachine(private val rules: List<TransitionRule>) {

    /** Current session state. Read-only from outside; mutated by [transition] and [restoreState]. */
    var state: SessionState = SessionState.Greeting
        private set

    private val _history = mutableListOf<StateTransition>()

    /** Ordered log of every successful transition this instance has performed. */
    val history: List<StateTransition> get() = _history.toList()

    /**
     * Attempts a state transition for [event].
     *
     * @return [Result.success] with the new state, or [Result.failure] if no rule matches.
     */
    fun transition(event: SessionEvent): Result<SessionState> {
        val rule = rules.firstOrNull { it.matches(state, event) }
            ?: return Result.failure(IllegalStateException("No transition from ${state.displayName} on ${event::class.simpleName}"))
        val nextState = rule.nextState(state, event)
        _history.add(StateTransition(from = state.displayName, to = nextState.displayName, event = event::class.simpleName ?: "Unknown"))
        state = nextState
        return Result.success(nextState)
    }

    fun canTransition(event: SessionEvent): Boolean =
        rules.any { it.matches(state, event) }

    fun reset() {
        state = SessionState.Greeting
        _history.clear()
    }

    fun restoreState(restoredState: SessionState) {
        state = restoredState
    }
}
