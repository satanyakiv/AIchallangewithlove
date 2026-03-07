package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.psy_agent.EnforceTaskPhaseUseCase
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.SessionEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskLifecycleEvent
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskPhase
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.TaskStateMachine
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.mindGuardTaskTransitions
import com.portfolio.ai_challenge.agent.psy_agent.statemachine.toStorageString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Day15TaskLifecycleTest {

    private fun machine() = TaskStateMachine(mindGuardTaskTransitions)

    // -- TaskStateMachine transitions -----------------------------------------

    @Test
    fun testTaskPhase_initialState_isAssessment() {
        assertEquals(TaskPhase.Assessment, machine().phase)
    }

    @Test
    fun testTaskPhase_assessmentComplete_movesToPlanProposed() {
        val m = machine()
        val result = m.transition(TaskLifecycleEvent.AssessmentComplete)
        assertTrue(result.isSuccess)
        assertTrue(m.phase is TaskPhase.PlanProposed)
    }

    @Test
    fun testTaskPhase_planApproved_movesToExecuting() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        val result = m.transition(TaskLifecycleEvent.PlanApproved)
        assertTrue(result.isSuccess)
        assertTrue(m.phase is TaskPhase.Executing)
    }

    @Test
    fun testTaskPhase_planRejected_backToAssessment() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        val result = m.transition(TaskLifecycleEvent.PlanRejected)
        assertTrue(result.isSuccess)
        assertEquals(TaskPhase.Assessment, m.phase)
    }

    @Test
    fun testTaskPhase_executionComplete_movesToValidating() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        val result = m.transition(TaskLifecycleEvent.ExecutionComplete)
        assertTrue(result.isSuccess)
        assertEquals(TaskPhase.Validating, m.phase)
    }

    @Test
    fun testTaskPhase_validationPassed_movesToCompleted() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        m.transition(TaskLifecycleEvent.ExecutionComplete)
        val result = m.transition(TaskLifecycleEvent.ValidationPassed)
        assertTrue(result.isSuccess)
        assertEquals(TaskPhase.Completed, m.phase)
    }

    @Test
    fun testTaskPhase_validationFailed_backToExecuting() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        m.transition(TaskLifecycleEvent.ExecutionComplete)
        val result = m.transition(TaskLifecycleEvent.ValidationFailed)
        assertTrue(result.isSuccess)
        assertTrue(m.phase is TaskPhase.Executing)
    }

    @Test
    fun testTaskPhase_fullLifecycle_happyPath() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        m.transition(TaskLifecycleEvent.ExecutionComplete)
        m.transition(TaskLifecycleEvent.ValidationPassed)
        assertEquals(TaskPhase.Completed, m.phase)
        assertEquals(4, m.history.size)
    }

    @Test
    fun testTaskPhase_history_recordsTransitions() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        assertEquals(1, m.history.size)
        assertEquals("assessment", m.history[0].from)
        assertEquals("plan_proposed", m.history[0].to)
        assertEquals("assessment_complete", m.history[0].event)
    }

    @Test
    fun testTaskPhase_allowedEvents_correctPerPhase() {
        val m = machine()
        // Assessment allows only AssessmentComplete
        val assessmentAllowed = m.allowedEvents()
        assertEquals(listOf("assessment_complete"), assessmentAllowed)

        m.transition(TaskLifecycleEvent.AssessmentComplete)
        val planAllowed = m.allowedEvents()
        assertTrue(planAllowed.contains("plan_approved"))
        assertTrue(planAllowed.contains("plan_rejected"))
    }

    // -- Skip prevention ------------------------------------------------------

    @Test
    fun testTaskPhase_assessmentToExecuting_blocked() {
        val m = machine()
        val result = m.transition(TaskLifecycleEvent.PlanApproved) // skip PlanProposed
        assertTrue(result.isFailure)
        assertEquals(TaskPhase.Assessment, m.phase)
    }

    @Test
    fun testTaskPhase_assessmentToCompleted_blocked() {
        val m = machine()
        val result = m.transition(TaskLifecycleEvent.ValidationPassed) // skip everything
        assertTrue(result.isFailure)
        assertEquals(TaskPhase.Assessment, m.phase)
    }

    @Test
    fun testTaskPhase_executingToCompleted_blocked() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        val result = m.transition(TaskLifecycleEvent.ValidationPassed) // skip Validating
        assertTrue(result.isFailure)
        assertTrue(m.phase is TaskPhase.Executing)
    }

    @Test
    fun testTaskPhase_planProposedToValidating_blocked() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        val result = m.transition(TaskLifecycleEvent.ExecutionComplete) // skip Executing
        assertTrue(result.isFailure)
        assertTrue(m.phase is TaskPhase.PlanProposed)
    }

    // -- EnforceTaskPhaseUseCase -----------------------------------------------

    private val enforcer = EnforceTaskPhaseUseCase()

    @Test
    fun testEnforcePhase_techniqueProposedInAssessment_blocked() {
        val result = enforcer.execute(TaskPhase.Assessment, SessionEvent.TechniqueProposed("breathing"))
        assertTrue(result is EnforceTaskPhaseUseCase.PhaseCheck.Blocked)
    }

    @Test
    fun testEnforcePhase_techniqueProposedInPlanProposed_allowed() {
        val result = enforcer.execute(TaskPhase.PlanProposed(), SessionEvent.TechniqueProposed("breathing"))
        assertEquals(EnforceTaskPhaseUseCase.PhaseCheck.Allowed, result)
    }

    @Test
    fun testEnforcePhase_sessionEndInExecuting_blocked() {
        val result = enforcer.execute(TaskPhase.Executing(), SessionEvent.SessionEndRequested)
        assertTrue(result is EnforceTaskPhaseUseCase.PhaseCheck.Blocked)
    }

    @Test
    fun testEnforcePhase_sessionEndInValidating_allowed() {
        val result = enforcer.execute(TaskPhase.Validating, SessionEvent.SessionEndRequested)
        assertEquals(EnforceTaskPhaseUseCase.PhaseCheck.Allowed, result)
    }

    @Test
    fun testEnforcePhase_userMessageInAnyPhase_allowed() {
        val msg = SessionEvent.UserMessage("hello there how are you")
        assertEquals(EnforceTaskPhaseUseCase.PhaseCheck.Allowed, enforcer.execute(TaskPhase.Assessment, msg))
        assertEquals(EnforceTaskPhaseUseCase.PhaseCheck.Allowed, enforcer.execute(TaskPhase.Executing(), msg))
        assertEquals(EnforceTaskPhaseUseCase.PhaseCheck.Allowed, enforcer.execute(TaskPhase.Completed, msg))
    }

    // -- Serialization roundtrips ----------------------------------------------

    @Test
    fun testTaskPhase_assessment_roundtrip() {
        val phase = TaskPhase.Assessment
        assertEquals(phase, TaskPhase.fromStorageString(phase.toStorageString()))
    }

    @Test
    fun testTaskPhase_planProposed_roundtrip() {
        val phase = TaskPhase.PlanProposed(plan = "breathing exercise")
        val restored = TaskPhase.fromStorageString(phase.toStorageString())
        assertTrue(restored is TaskPhase.PlanProposed)
        assertEquals("breathing exercise", (restored as TaskPhase.PlanProposed).plan)
    }

    @Test
    fun testTaskPhase_executing_roundtrip() {
        val phase = TaskPhase.Executing(plan = "box breathing")
        val restored = TaskPhase.fromStorageString(phase.toStorageString())
        assertTrue(restored is TaskPhase.Executing)
        assertEquals("box breathing", (restored as TaskPhase.Executing).plan)
    }

    @Test
    fun testTaskPhase_validating_roundtrip() {
        val phase = TaskPhase.Validating
        assertEquals(phase, TaskPhase.fromStorageString(phase.toStorageString()))
    }

    @Test
    fun testTaskPhase_completed_roundtrip() {
        val phase = TaskPhase.Completed
        assertEquals(phase, TaskPhase.fromStorageString(phase.toStorageString()))
    }

    // -- Pause/Resume ----------------------------------------------------------

    @Test
    fun testTaskPhase_pauseAndResume_correctPhase() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        m.transition(TaskLifecycleEvent.PlanApproved)
        val stored = m.phase.toStorageString()

        // Simulate new machine (after pause)
        val m2 = machine()
        m2.restorePhase(TaskPhase.fromStorageString(stored))
        assertTrue(m2.phase is TaskPhase.Executing)
        assertTrue(m2.canTransition(TaskLifecycleEvent.ExecutionComplete))
        assertFalse(m2.canTransition(TaskLifecycleEvent.ValidationPassed)) // can't skip
    }

    @Test
    fun testTaskPhase_resumeAfterPlanProposed_allowsApproval() {
        val m = machine()
        m.transition(TaskLifecycleEvent.AssessmentComplete)
        val stored = m.phase.toStorageString()

        val m2 = machine()
        m2.restorePhase(TaskPhase.fromStorageString(stored))
        assertTrue(m2.phase is TaskPhase.PlanProposed)
        val result = m2.transition(TaskLifecycleEvent.PlanApproved)
        assertTrue(result.isSuccess)
        assertTrue(m2.phase is TaskPhase.Executing)
    }

    // -- ContextStore task phase persistence ------------------------------------

    @Test
    fun testContextStore_updateAndLoadTaskPhase() {
        val store = com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore()
        val sessionId = "test-session"
        store.createSession(sessionId, "user1")
        store.updateTaskPhase(sessionId, "executing:breathing")
        assertEquals("executing:breathing", store.loadTaskPhase(sessionId))
    }

    @Test
    fun testContextStore_loadTaskPhase_unknownSession_returnsNull() {
        val store = com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore()
        assertEquals(null, store.loadTaskPhase("nonexistent"))
    }

    @Test
    fun testContextStore_updateTaskPhase_overwritesPrevious() {
        val store = com.portfolio.ai_challenge.agent.psy_agent.memory.InMemoryContextStore()
        val sessionId = "test-session"
        store.createSession(sessionId, "user1")
        store.updateTaskPhase(sessionId, "assessment")
        store.updateTaskPhase(sessionId, "plan_proposed:relaxation")
        assertEquals("plan_proposed:relaxation", store.loadTaskPhase(sessionId))
    }
}
