package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.DischargePatientUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.DischargePatientRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.DischargeDialog
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakePractitionerCareLinkRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf

class DischargePatientViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakePractitionerCareLinkRepository()

    private fun viewModel() = DischargePatientViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(DischargePatientRoute.ARG_CARE_LINK_ID to 8L, DischargePatientRoute.ARG_PATIENT_NAME to "Ana Flores"),
        ),
        dischargePatient = DischargePatientUseCase(careLinks),
        connectivityObserver = FakeConnectivityObserver(),
    )

    @Test
    fun `the clinical reason is required and blank text is not a reason`() {
        assertNull(ClinicalReason.of("   "))
        assertEquals("Objetivos alcanzados", ClinicalReason.of(" Objetivos alcanzados ")?.text)
    }

    @Test
    fun `without a reason the confirmation does not open`() {
        val viewModel = viewModel()

        viewModel.onDischargeRequested()

        assertTrue(viewModel.state.value.reasonMissing)
        assertNull(viewModel.state.value.dialog)
        assertTrue(careLinks.discharges.isEmpty())
    }

    @Test
    fun `PR18 M confirms and then discharges with the reason`() = runTest {
        val viewModel = viewModel()
        viewModel.onReasonChange("Objetivos alcanzados")
        viewModel.onDischargeRequested()
        assertEquals(DischargeDialog.CONFIRM, viewModel.state.value.dialog)
        assertTrue(careLinks.discharges.isEmpty())

        viewModel.events.test {
            viewModel.onConfirmed()
            assertEquals(DischargePatientEvent.Discharged("Ana Flores"), awaitItem())
        }
        assertEquals(listOf(8L to "Objetivos alcanzados"), careLinks.discharges)
    }

    @Test
    fun `an already discharged link says so and a server error offers to retry`() {
        careLinks.dischargeResult = failureOf(DomainError.Conflict("DischargedLinkCannotBeReactivated"))
        val viewModel = viewModel()
        viewModel.onReasonChange("Objetivos alcanzados")
        viewModel.onConfirmed()
        assertEquals(DischargeDialog.ALREADY_DISCHARGED, viewModel.state.value.dialog)

        careLinks.dischargeResult = failureOf(DomainError.Network)
        viewModel.onConfirmed()
        assertEquals(DischargeDialog.SERVER_ERROR, viewModel.state.value.dialog)
    }
}
