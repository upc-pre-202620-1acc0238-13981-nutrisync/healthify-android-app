package pe.edu.upc.healthify.features.main.presentation.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

class PatientHomeUiStateTest {

    private val targets = HomeTargets(planVersion = 1, energyKcal = 1850.0, proteinG = 90.0, carbG = 238.0, fatG = 60.0)
    private val base = PatientHomeUiState(isLoading = false, targets = targets)

    @Test
    fun `an EXCEEDED day shows the energy card and compacts the register button`() {
        val state = base.copy(consumedKcal = 2100.0, todayOutcome = ComplianceOutcome.EXCEEDED)

        assertTrue(state.energyExceededCardVisible)
        assertTrue(state.compactRegisterButton)
    }

    @Test
    fun `consumed energy above the target shows the card even before the outcome says so`() {
        val state = base.copy(consumedKcal = 1900.0, todayOutcome = ComplianceOutcome.MET)

        assertTrue(state.energyExceededCardVisible)
    }

    @Test
    fun `within the target there is no card`() {
        val state = base.copy(consumedKcal = 1850.0, todayOutcome = ComplianceOutcome.MET)

        assertFalse(state.energyExceededCardVisible)
        assertFalse(state.compactRegisterButton)
    }

    @Test
    fun `offline or without targets the card never appears`() {
        val exceeded = base.copy(consumedKcal = 2100.0, todayOutcome = ComplianceOutcome.EXCEEDED)

        assertFalse(exceeded.copy(isOffline = true).energyExceededCardVisible)
        assertFalse(exceeded.copy(targets = null).energyExceededCardVisible)
    }
}
