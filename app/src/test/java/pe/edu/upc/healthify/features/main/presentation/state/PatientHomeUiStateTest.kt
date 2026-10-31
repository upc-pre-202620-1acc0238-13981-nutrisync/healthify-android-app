package pe.edu.upc.healthify.features.main.presentation.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome

class PatientHomeUiStateTest {

    private val targets = HomeTargets(planVersion = 1, energyKcal = 1850.0, proteinG = 90.0, carbG = 238.0, fatG = 60.0)
    private val base = PatientHomeUiState(isLoading = false, targets = targets)

    @Test
    fun `an EXCEEDED day shows the energy card`() {
        val state = base.copy(consumedKcal = 2100.0, todayOutcome = ComplianceOutcome.EXCEEDED)

        assertTrue(state.energyExceededCardVisible)
    }

    @Test
    fun `above the target but within the tolerance the day is met, with no card`() {
        val state = base.copy(consumedKcal = 1900.0, todayOutcome = ComplianceOutcome.MET)

        assertFalse(state.energyExceededCardVisible)
        assertTrue(state.energyTargetReached)
    }

    @Test
    fun `an EXCEEDED day is not shown as target met`() {
        val state = base.copy(consumedKcal = 2100.0, todayOutcome = ComplianceOutcome.EXCEEDED)

        assertFalse(state.energyTargetReached)
    }

    @Test
    fun `before the outcome arrives the tolerance band decides`() {
        val withinBand = base.copy(consumedKcal = 1900.0)
        val aboveBand = base.copy(consumedKcal = 2100.0)

        assertTrue(withinBand.energyTargetReached)
        assertFalse(withinBand.energyExceededCardVisible)
        assertFalse(aboveBand.energyTargetReached)
        assertTrue(aboveBand.energyExceededCardVisible)
    }

    @Test
    fun `below the target the gauge keeps counting down`() {
        val state = base.copy(consumedKcal = 1700.0, todayOutcome = ComplianceOutcome.MET)

        assertFalse(state.energyTargetReached)
        assertFalse(state.energyExceededCardVisible)
    }

    @Test
    fun `within the target there is no card`() {
        val state = base.copy(consumedKcal = 1850.0, todayOutcome = ComplianceOutcome.MET)

        assertFalse(state.energyExceededCardVisible)
    }

    @Test
    fun `offline or without targets the card never appears`() {
        val exceeded = base.copy(consumedKcal = 2100.0, todayOutcome = ComplianceOutcome.EXCEEDED)

        assertFalse(exceeded.copy(isOffline = true).energyExceededCardVisible)
        assertFalse(exceeded.copy(targets = null).energyExceededCardVisible)
    }
}
