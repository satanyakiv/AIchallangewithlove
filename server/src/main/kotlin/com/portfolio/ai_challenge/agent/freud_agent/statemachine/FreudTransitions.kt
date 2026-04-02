package com.portfolio.ai_challenge.agent.freud_agent.statemachine

val freudTransitions: List<FreudTransitionRule> = listOf(
    // 1. Begruessung + PatientMessage (long) -> FreeAssociation
    FreudTransitionRule(
        fromState = FreudSessionState.Begruessung::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        guard = { _, evt -> (evt as FreudSessionEvent.PatientMessage).content.length > 10 },
        nextState = { _, _ -> FreudSessionState.FreeAssociation(turnCount = 1) },
    ),

    // 2. FreeAssociation + PatientMessage (turnCount < 3) -> increment turn
    FreudTransitionRule(
        fromState = FreudSessionState.FreeAssociation::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        guard = { state, _ -> (state as FreudSessionState.FreeAssociation).turnCount < 3 },
        nextState = { state, _ ->
            val s = state as FreudSessionState.FreeAssociation
            s.copy(turnCount = s.turnCount + 1)
        },
    ),

    // 3. FreeAssociation + DreamDetected -> DreamAnalysis
    FreudTransitionRule(
        fromState = FreudSessionState.FreeAssociation::class,
        eventType = FreudSessionEvent.DreamDetected::class,
        nextState = { _, _ -> FreudSessionState.DreamAnalysis(dreamCount = 1) },
    ),

    // 4. FreeAssociation + ResistanceDetected -> Transference
    FreudTransitionRule(
        fromState = FreudSessionState.FreeAssociation::class,
        eventType = FreudSessionEvent.ResistanceDetected::class,
        nextState = { _, _ -> FreudSessionState.Transference },
    ),

    // 5. FreeAssociation + PatientMessage (turnCount >= 3) -> Interpretation
    FreudTransitionRule(
        fromState = FreudSessionState.FreeAssociation::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        guard = { state, _ -> (state as FreudSessionState.FreeAssociation).turnCount >= 3 },
        nextState = { _, _ -> FreudSessionState.Interpretation(topic = "general") },
    ),

    // 6. DreamAnalysis + PatientMessage -> Interpretation(dream)
    FreudTransitionRule(
        fromState = FreudSessionState.DreamAnalysis::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        nextState = { _, _ -> FreudSessionState.Interpretation(topic = "dream") },
    ),

    // 7. Interpretation + PatientMessage -> FreeAssociation(reset)
    FreudTransitionRule(
        fromState = FreudSessionState.Interpretation::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        nextState = { _, _ -> FreudSessionState.FreeAssociation(turnCount = 0) },
    ),

    // 8. Transference + PatientMessage -> FreeAssociation(reset)
    FreudTransitionRule(
        fromState = FreudSessionState.Transference::class,
        eventType = FreudSessionEvent.PatientMessage::class,
        nextState = { _, _ -> FreudSessionState.FreeAssociation(turnCount = 0) },
    ),

    // 9. ANY (not Abschluss) + SessionEndRequested -> Abschluss
    FreudTransitionRule(
        fromState = FreudSessionState::class,
        eventType = FreudSessionEvent.SessionEndRequested::class,
        anyState = true,
        guard = { state, _ -> state !is FreudSessionState.Abschluss },
        nextState = { _, _ -> FreudSessionState.Abschluss },
    ),
)
