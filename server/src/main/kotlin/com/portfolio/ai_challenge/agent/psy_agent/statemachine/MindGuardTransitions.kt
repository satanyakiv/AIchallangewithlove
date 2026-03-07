package com.portfolio.ai_challenge.agent.psy_agent.statemachine

/**
 * All 13 transition rules for the MindGuard session state machine.
 * Rules are evaluated in order — first match wins.
 */
val mindGuardTransitions: List<TransitionRule> = listOf(
    // (a) Greeting → ActiveListening when user sends a meaningful message
    TransitionRule(
        fromState = SessionState.Greeting::class,
        event = SessionEvent.UserMessage::class,
        guard = { _, evt -> (evt as SessionEvent.UserMessage).content.length > 10 && !evt.hasCrisisIndicators },
        nextState = { _, _ -> SessionState.ActiveListening(turnCount = 1) },
    ),

    // (b) ActiveListening → ActiveListening (increment turn count)
    TransitionRule(
        fromState = SessionState.ActiveListening::class,
        event = SessionEvent.UserMessage::class,
        guard = { _, evt -> !(evt as SessionEvent.UserMessage).hasCrisisIndicators },
        nextState = { state, _ ->
            val s = state as SessionState.ActiveListening
            s.copy(turnCount = s.turnCount + 1)
        },
    ),

    // (c) ActiveListening → Intervention when technique is proposed
    TransitionRule(
        fromState = SessionState.ActiveListening::class,
        event = SessionEvent.TechniqueProposed::class,
        nextState = { _, evt ->
            val e = evt as SessionEvent.TechniqueProposed
            SessionState.Intervention(technique = e.technique, step = 0, totalSteps = e.totalSteps)
        },
    ),

    // (d) Intervention — TechniqueAccepted starts step 1
    TransitionRule(
        fromState = SessionState.Intervention::class,
        event = SessionEvent.TechniqueAccepted::class,
        nextState = { state, _ ->
            val s = state as SessionState.Intervention
            s.copy(step = 1)
        },
    ),

    // (e) Intervention — StepCompleted increments step (if not last)
    TransitionRule(
        fromState = SessionState.Intervention::class,
        event = SessionEvent.StepCompleted::class,
        guard = { state, _ -> (state as SessionState.Intervention).step < state.totalSteps },
        nextState = { state, _ ->
            val s = state as SessionState.Intervention
            s.copy(step = s.step + 1)
        },
    ),

    // (f) Intervention → ActiveListening when technique completed
    TransitionRule(
        fromState = SessionState.Intervention::class,
        event = SessionEvent.TechniqueCompleted::class,
        nextState = { _, _ -> SessionState.ActiveListening() },
    ),

    // (g) ActiveListening → Closing when end requested
    TransitionRule(
        fromState = SessionState.ActiveListening::class,
        event = SessionEvent.SessionEndRequested::class,
        nextState = { _, _ -> SessionState.Closing() },
    ),

    // (h) Intervention → Closing when end requested
    TransitionRule(
        fromState = SessionState.Intervention::class,
        event = SessionEvent.SessionEndRequested::class,
        nextState = { _, _ -> SessionState.Closing() },
    ),

    // (i) Closing → Finished
    TransitionRule(
        fromState = SessionState.Closing::class,
        event = SessionEvent.SessionEndRequested::class,
        nextState = { _, _ -> SessionState.Finished },
    ),

    // (j) ANY state → CrisisMode when crisis detected
    TransitionRule(
        fromState = SessionState::class,
        event = SessionEvent.CrisisDetected::class,
        anyState = true,
        guard = { state, _ -> state !is SessionState.Finished },
        nextState = { _, evt ->
            val e = evt as SessionEvent.CrisisDetected
            SessionState.CrisisMode(riskLevel = e.riskLevel, escalatedAt = System.currentTimeMillis())
        },
    ),

    // (k) CrisisMode → ActiveListening when resolved (after 5 min cooldown, no indicators)
    TransitionRule(
        fromState = SessionState.CrisisMode::class,
        event = SessionEvent.CrisisResolved::class,
        guard = { state, _ ->
            val s = state as SessionState.CrisisMode
            (System.currentTimeMillis() - s.escalatedAt) > 300_000L
        },
        nextState = { _, _ -> SessionState.ActiveListening() },
    ),

    // (l) CrisisMode — block SessionEndRequested (cannot close during crisis)
    // No rule means transition fails — canTransition returns false

    // (m) Greeting — short message stays in Greeting (no transition)
    // Handled implicitly: UserMessage with length <= 10 doesn't match rule (a)
)
