package pe.edu.upc.healthify.features.main.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.RequestConsultationRemindersUseCase
import pe.edu.upc.healthify.testing.FakeConsultationReminderScheduler
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule

class PatientShellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reminders = FakeConsultationReminderScheduler()
    private val viewModel by lazy {
        PatientShellViewModel(ObserveCurrentUserUseCase(FakeSessionRepository()), RequestConsultationRemindersUseCase(reminders))
    }

    @Test
    fun `entering the shell reschedules the consultation notices`() {
        viewModel.state.value

        assertEquals(1, reminders.refreshRequests)
    }

    @Test
    fun `the menu header shows the session user`() {
        val state = viewModel.state.value

        assertEquals("María Flores", state.fullName)
        assertEquals("M", state.avatarInitial)
    }

    @Test
    fun `the menu starts closed, opens and closes`() {
        assertFalse(viewModel.state.value.isMenuOpen)

        viewModel.onOpenMenu()
        assertTrue(viewModel.state.value.isMenuOpen)

        viewModel.onCloseMenu()
        assertFalse(viewModel.state.value.isMenuOpen)
    }
}
