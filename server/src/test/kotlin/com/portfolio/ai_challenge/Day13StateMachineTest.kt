package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.day_11_psy_agent.DetectCrisisUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.DetermineIntentUseCase
import com.portfolio.ai_challenge.agent.day_11_psy_agent.model.TurnContext
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionState
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.SessionStateMachine
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.mindGuardTransitions
import com.portfolio.ai_challenge.agent.day_11_psy_agent.statemachine.toStorageString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Day13StateMachineTest {

    private fun machine() = SessionStateMachine(mindGuardTransitions)

    // ── State Machine Unit Tests ──────────────────────────────────────────────

    @Test
    fun testStateMachine_initialState_isGreeting() {
        assertEquals(SessionState.Greeting, machine().state)
    }

    @Test
    fun testStateMachine_greetingWithLongMessage_transitionsToActiveListening() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        assertTrue(m.state is SessionState.ActiveListening)
    }

    @Test
    fun testStateMachine_greetingWithShortMessage_staysInGreeting() {
        val m = machine()
        val result = m.transition(SessionEvent.UserMessage("hi"))
        assertTrue(result.isFailure) // no rule matches short non-crisis message from Greeting
        assertEquals(SessionState.Greeting, m.state)
    }

    @Test
    fun testStateMachine_activeListeningTurnCount_increments() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.UserMessage("It has been going on for a while now"))
        val state = m.state as SessionState.ActiveListening
        assertEquals(2, state.turnCount)
    }

    @Test
    fun testStateMachine_techniqueProposed_transitionsToIntervention() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.TechniqueProposed("breathing", totalSteps = 3))
        val state = m.state as SessionState.Intervention
        assertEquals("breathing", state.technique)
    }

    @Test
    fun testStateMachine_techniqueCompleted_returnsToActiveListening() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.TechniqueProposed("breathing", 3))
        m.transition(SessionEvent.TechniqueCompleted)
        assertTrue(m.state is SessionState.ActiveListening)
    }

    @Test
    fun testStateMachine_sessionEndRequested_transitionsToClosing() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.SessionEndRequested)
        assertTrue(m.state is SessionState.Closing)
    }

    @Test
    fun testStateMachine_closingThenEnd_transitionsToFinished() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.SessionEndRequested)
        m.transition(SessionEvent.SessionEndRequested)
        assertEquals(SessionState.Finished, m.state)
    }

    @Test
    fun testStateMachine_history_recordsTransitions() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        assertEquals(1, m.history.size)
        assertEquals("greeting", m.history[0].from)
        assertEquals("active_listening", m.history[0].to)
    }

    @Test
    fun testStateMachine_fullLifecycle_historySize() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.TechniqueProposed("breathing", 3))
        m.transition(SessionEvent.TechniqueCompleted)
        m.transition(SessionEvent.SessionEndRequested)
        m.transition(SessionEvent.SessionEndRequested)
        assertEquals(5, m.history.size)
        assertEquals("finished", m.state.displayName)
    }

    @Test
    fun testStateMachine_reset_returnsToGreeting() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.reset()
        assertEquals(SessionState.Greeting, m.state)
        assertTrue(m.history.isEmpty())
    }

    // ── Crisis Tests ──────────────────────────────────────────────────────────

    @Test
    fun testStateMachine_crisisFromGreeting_entersCrisisMode() {
        val m = machine()
        m.transition(SessionEvent.CrisisDetected("high", listOf("suicide")))
        assertTrue(m.state is SessionState.CrisisMode)
    }

    @Test
    fun testStateMachine_crisisFromActiveListening_entersCrisisMode() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.CrisisDetected("high", listOf("want to die")))
        assertTrue(m.state is SessionState.CrisisMode)
    }

    @Test
    fun testStateMachine_crisisFromIntervention_entersCrisisMode() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.TechniqueProposed("breathing", 3))
        m.transition(SessionEvent.CrisisDetected("high", listOf("kill myself")))
        assertTrue(m.state is SessionState.CrisisMode)
    }

    @Test
    fun testStateMachine_cannotCloseInCrisis() {
        val m = machine()
        m.transition(SessionEvent.CrisisDetected("high", listOf("suicide")))
        assertFalse(m.canTransition(SessionEvent.SessionEndRequested))
    }

    @Test
    fun testStateMachine_crisisEarlyDeescalation_blocked() {
        val m = machine()
        m.transition(SessionEvent.CrisisDetected("high", listOf("suicide")))
        // Try to resolve immediately — guard requires 5 min cooldown
        val result = m.transition(SessionEvent.CrisisResolved)
        assertTrue(result.isFailure)
        assertTrue(m.state is SessionState.CrisisMode)
    }

    @Test
    fun testStateMachine_finishedState_noTransitions() {
        val m = machine()
        m.transition(SessionEvent.UserMessage("I am feeling really anxious lately"))
        m.transition(SessionEvent.SessionEndRequested)
        m.transition(SessionEvent.SessionEndRequested)
        assertEquals(SessionState.Finished, m.state)
        val result = m.transition(SessionEvent.UserMessage("Hi again, I am feeling anxious"))
        assertTrue(result.isFailure)
    }

    @Test
    fun testStateMachine_crisisFinished_noTransitions() {
        val m = machine()
        m.restoreState(SessionState.Finished)
        val result = m.transition(SessionEvent.CrisisDetected("high", listOf("suicide")))
        // guard: state !is Finished prevents transition
        assertTrue(result.isFailure)
    }

    // ── DetectCrisisUseCase Tests ─────────────────────────────────────────────

    @Test
    fun testDetectCrisis_suicideKeyword_positive() {
        val result = DetectCrisisUseCase().execute("I want to commit suicide")
        assertTrue(result.isPositive)
        assertEquals("high", result.level)
    }

    @Test
    fun testDetectCrisis_selfHarmKeyword_positive() {
        val result = DetectCrisisUseCase().execute("I want to hurt myself")
        assertTrue(result.isPositive)
    }

    @Test
    fun testDetectCrisis_normalSadMessage_negative() {
        val result = DetectCrisisUseCase().execute("I feel sad and lonely today")
        assertFalse(result.isPositive)
        assertEquals("none", result.level)
    }

    @Test
    fun testDetectCrisis_caseInsensitive() {
        val result = DetectCrisisUseCase().execute("I WANT TO KILL MYSELF")
        assertTrue(result.isPositive)
    }

    @Test
    fun testDetectCrisis_multipleIndicators_allFound() {
        val result = DetectCrisisUseCase().execute("I want to kill myself and end my life")
        assertTrue(result.isPositive)
        assertTrue(result.indicators.size >= 2)
    }

    // ── DetermineIntentUseCase Tests ──────────────────────────────────────────

    @Test
    fun testDetermineIntent_greeting_returnsWelcome() {
        val intent = DetermineIntentUseCase().execute(SessionState.Greeting, TurnContext())
        assertEquals("welcome", intent)
    }

    @Test
    fun testDetermineIntent_activeListening_returnsActiveListening() {
        val intent = DetermineIntentUseCase().execute(SessionState.ActiveListening(), TurnContext())
        assertEquals("active_listening", intent)
    }

    @Test
    fun testDetermineIntent_intervention_includesStep() {
        val intent = DetermineIntentUseCase().execute(SessionState.Intervention("breathing", step = 2), TurnContext())
        assertEquals("intervention_step_2", intent)
    }

    @Test
    fun testDetermineIntent_crisis_returnsCrisisSupport() {
        val intent = DetermineIntentUseCase().execute(SessionState.CrisisMode("high", System.currentTimeMillis()), TurnContext())
        assertEquals("crisis_support", intent)
    }

    @Test
    fun testDetermineIntent_closing_returnsSessionClosing() {
        val intent = DetermineIntentUseCase().execute(SessionState.Closing(), TurnContext())
        assertEquals("session_closing", intent)
    }

    @Test
    fun testDetermineIntent_finished_returnsSessionFinished() {
        val intent = DetermineIntentUseCase().execute(SessionState.Finished, TurnContext())
        assertEquals("session_finished", intent)
    }

    // ── State Serialization Tests ─────────────────────────────────────────────

    @Test
    fun testStorageString_greeting_roundtrip() {
        val state = SessionState.Greeting
        val restored = SessionState.Companion.fromStorageString(state.toStorageString())
        assertEquals(state, restored)
    }

    @Test
    fun testStorageString_activeListening_roundtrip() {
        val state = SessionState.ActiveListening(turnCount = 5)
        val restored = SessionState.Companion.fromStorageString(state.toStorageString())
        assertEquals(state, restored)
    }

    @Test
    fun testStorageString_intervention_roundtrip() {
        val state = SessionState.Intervention(technique = "breathing", step = 2, totalSteps = 4)
        val restored = SessionState.Companion.fromStorageString(state.toStorageString())
        assertEquals(state, restored)
    }

    @Test
    fun testStorageString_closing_roundtrip() {
        val state = SessionState.Closing()
        val restored = SessionState.Companion.fromStorageString(state.toStorageString())
        assertTrue(restored is SessionState.Closing)
    }

    @Test
    fun testStorageString_finished_roundtrip() {
        val state = SessionState.Finished
        val restored = SessionState.Companion.fromStorageString(state.toStorageString())
        assertEquals(state, restored)
    }

    @Test
    fun testStorageString_unknown_fallbackToGreeting() {
        val restored = SessionState.Companion.fromStorageString("unknown_state")
        assertEquals(SessionState.Greeting, restored)
    }
}
