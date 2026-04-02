package com.portfolio.ai_challenge

import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionEvent
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudSessionState
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.FreudStateMachine
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.freudTransitions
import com.portfolio.ai_challenge.agent.freud_agent.statemachine.toStorageString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FreudStateMachineTest {

    private fun createMachine() = FreudStateMachine(freudTransitions)

    @Test
    fun testInitialState_isBegruessung() {
        val machine = createMachine()
        assertIs<FreudSessionState.Begruessung>(machine.state)
    }

    @Test
    fun testBegruessung_longMessage_transitionsToFreeAssociation() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been feeling very anxious lately"))
        assertIs<FreudSessionState.FreeAssociation>(machine.state)
        assertEquals(1, (machine.state as FreudSessionState.FreeAssociation).turnCount)
    }

    @Test
    fun testBegruessung_shortMessage_staysInBegruessung() {
        val machine = createMachine()
        val result = machine.transition(FreudSessionEvent.PatientMessage("hi"))
        assertTrue(result.isFailure)
        assertIs<FreudSessionState.Begruessung>(machine.state)
    }

    @Test
    fun testFreeAssociation_normalMessage_incrementsTurnCount() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about my childhood a lot"))
        machine.transition(FreudSessionEvent.PatientMessage("It bothers me"))
        val state = machine.state as FreudSessionState.FreeAssociation
        assertEquals(2, state.turnCount)
    }

    @Test
    fun testFreeAssociation_dreamDetected_transitionsToDreamAnalysis() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.DreamDetected)
        assertIs<FreudSessionState.DreamAnalysis>(machine.state)
    }

    @Test
    fun testFreeAssociation_resistanceDetected_transitionsToTransference() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.ResistanceDetected)
        assertIs<FreudSessionState.Transference>(machine.state)
    }

    @Test
    fun testFreeAssociation_thirdTurn_transitionsToInterpretation() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been feeling anxious about everything"))
        machine.transition(FreudSessionEvent.PatientMessage("It gets worse at night"))
        machine.transition(FreudSessionEvent.PatientMessage("I can not sleep at all"))
        machine.transition(FreudSessionEvent.PatientMessage("And my mother called again"))
        assertIs<FreudSessionState.Interpretation>(machine.state)
    }

    @Test
    fun testDreamAnalysis_message_transitionsToInterpretation() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.DreamDetected)
        machine.transition(FreudSessionEvent.PatientMessage("The dream was about flying"))
        val state = machine.state as FreudSessionState.Interpretation
        assertEquals("dream", state.topic)
    }

    @Test
    fun testInterpretation_message_returnsToFreeAssociation() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.DreamDetected)
        machine.transition(FreudSessionEvent.PatientMessage("The dream was scary"))
        assertIs<FreudSessionState.Interpretation>(machine.state)
        machine.transition(FreudSessionEvent.PatientMessage("I see"))
        assertIs<FreudSessionState.FreeAssociation>(machine.state)
        assertEquals(0, (machine.state as FreudSessionState.FreeAssociation).turnCount)
    }

    @Test
    fun testTransference_message_returnsToFreeAssociation() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.ResistanceDetected)
        assertIs<FreudSessionState.Transference>(machine.state)
        machine.transition(FreudSessionEvent.PatientMessage("Fine, whatever"))
        assertIs<FreudSessionState.FreeAssociation>(machine.state)
    }

    @Test
    fun testAnyState_sessionEndRequested_transitionsToAbschluss() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.SessionEndRequested)
        assertIs<FreudSessionState.Abschluss>(machine.state)
    }

    @Test
    fun testAbschluss_sessionEndRequested_staysInAbschluss() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.SessionEndRequested)
        assertIs<FreudSessionState.Abschluss>(machine.state)
        val result = machine.transition(FreudSessionEvent.SessionEndRequested)
        assertTrue(result.isFailure)
    }

    @Test
    fun testStorageString_roundtrip_begruessung() {
        val state = FreudSessionState.Begruessung
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testStorageString_roundtrip_freeAssociation() {
        val state = FreudSessionState.FreeAssociation(turnCount = 2)
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testStorageString_roundtrip_interpretation() {
        val state = FreudSessionState.Interpretation(topic = "dream")
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testStorageString_roundtrip_dreamAnalysis() {
        val state = FreudSessionState.DreamAnalysis(dreamCount = 3)
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testStorageString_roundtrip_transference() {
        val state = FreudSessionState.Transference
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testStorageString_roundtrip_abschluss() {
        val state = FreudSessionState.Abschluss
        assertEquals(state, FreudSessionState.fromStorageString(state.toStorageString()))
    }

    @Test
    fun testHistory_tracksTransitions() {
        val machine = createMachine()
        machine.transition(FreudSessionEvent.PatientMessage("I have been thinking about many things lately"))
        machine.transition(FreudSessionEvent.DreamDetected)
        assertEquals(2, machine.history.size)
        assertEquals("begruessung", machine.history[0].from)
        assertEquals("free_association", machine.history[0].to)
        assertEquals("free_association", machine.history[1].from)
        assertEquals("dream_analysis", machine.history[1].to)
    }
}
