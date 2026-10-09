package pe.edu.upc.healthify.features.iam.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientLinkStatusUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ResolveNavigationShellUseCase
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.presentation.state.ShellDestination
import pe.edu.upc.healthify.features.iam.presentation.state.ShellLoadingEvent
import pe.edu.upc.healthify.features.iam.presentation.state.selectShellDestination
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.sessionUser

class ShellLoadingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthenticationRepository()
    private val careLinks = FakeCareLinkRepository().apply {
        statusResults = listOf(Result.success(PatientLinkStatus.ACTIVE))
    }
    private var session = FakeSessionRepository(sessionUser(UserRole.PATIENT, id = 21))

    private fun viewModel() = ShellLoadingViewModel(
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        resolveNavigationShell = ResolveNavigationShellUseCase(auth),
        getPatientLinkStatus = GetPatientLinkStatusUseCase(careLinks),
    )

    @Test
    fun `shell selection rule`() {
        assertEquals(ShellDestination.PractitionerHome, selectShellDestination(NavigationShell.PRACTITIONER, null))
        assertEquals(
            ShellDestination.PractitionerHome,
            selectShellDestination(NavigationShell.PRACTITIONER, PatientLinkStatus.ACTIVE),
        )
        assertEquals(ShellDestination.PatientHome, selectShellDestination(NavigationShell.PATIENT, PatientLinkStatus.ACTIVE))
        assertEquals(
            ShellDestination.PatientPendingConsent,
            selectShellDestination(NavigationShell.PATIENT, PatientLinkStatus.PENDING_CONSENT),
        )
        assertEquals(
            ShellDestination.PatientScanInvitation,
            selectShellDestination(NavigationShell.PATIENT, PatientLinkStatus.NO_LINK),
        )
    }

    @Test
    fun `patient with active link goes to PT3`() = runTest {
        expectDestination(ShellDestination.PatientHome)
        assertEquals(listOf(PatientId(21)), careLinks.requestedPatients)
    }

    @Test
    fun `patient without link goes to PT1`() = runTest {
        careLinks.statusResults = listOf(Result.success(PatientLinkStatus.NO_LINK))
        expectDestination(ShellDestination.PatientScanInvitation)
    }

    @Test
    fun `patient with pending link goes to PT2_1`() = runTest {
        careLinks.statusResults = listOf(Result.success(PatientLinkStatus.PENDING_CONSENT))
        expectDestination(ShellDestination.PatientPendingConsent)
    }

    @Test
    fun `practitioner goes to PR1 without asking for a care link`() = runTest {
        session = FakeSessionRepository(sessionUser(UserRole.PRACTITIONER))
        auth.navigationShellResults = listOf(Result.success(NavigationShell.PRACTITIONER))

        expectDestination(ShellDestination.PractitionerHome)
        assertTrue(careLinks.requestedPatients.isEmpty())
    }

    @Test
    fun `a failure is retried silently once`() = runTest {
        careLinks.statusResults = listOf(failureOf(DomainError.Unexpected("InternalError")), Result.success(PatientLinkStatus.ACTIVE))

        expectDestination(ShellDestination.PatientHome)
        assertEquals(2, careLinks.requestedPatients.size)
    }

    @Test
    fun `a persistent failure shows the error state and Retry loads again`() = runTest {
        auth.navigationShellResults = listOf(failureOf(DomainError.Unexpected("InternalError")))
        val viewModel = viewModel()

        viewModel.events.test {
            testScheduler.advanceUntilIdle()
            assertTrue(viewModel.state.value.isError)
            assertEquals(2, auth.navigationShellCalls)

            auth.navigationShellResults = listOf(Result.success(NavigationShell.PATIENT))
            viewModel.onRetry()
            assertEquals(ShellLoadingEvent.Navigate(ShellDestination.PatientHome), awaitItem())
        }
    }

    @Test
    fun `without a saved user goes back to S2`() = runTest {
        session = FakeSessionRepository(user = null)
        val viewModel = viewModel()

        viewModel.events.test {
            assertEquals(ShellLoadingEvent.NavigateToWelcome, awaitItem())
        }
    }

    private suspend fun expectDestination(destination: ShellDestination) {
        val viewModel = viewModel()
        viewModel.events.test {
            assertEquals(ShellLoadingEvent.Navigate(destination), awaitItem())
        }
        assertFalse(viewModel.state.value.isError)
    }
}
