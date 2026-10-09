package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPendingCareLinkUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.WithdrawConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.presentation.state.PendingConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.WithdrawConsentEvent
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.SignOutUseCase
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf

class WithdrawAndPendingConsentViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakeCareLinkRepository()
    private val session = FakeSessionRepository()
    private val connectivity = FakeConnectivityObserver()

    private val withdraw by lazy {
        WithdrawConsentViewModel(ObserveCurrentUserUseCase(session), WithdrawConsentUseCase(careLinks), connectivity)
    }
    private val pending by lazy {
        PendingConsentViewModel(
            ObserveCurrentUserUseCase(session),
            GetPendingCareLinkUseCase(careLinks),
            SignOutUseCase(FakeAuthenticationRepository(), session),
        )
    }

    @Test
    fun `PT23 asks for confirmation before withdrawing`() {
        withdraw.onWithdrawClick()

        assertTrue(withdraw.state.value.showConfirmDialog)
        assertTrue(careLinks.withdrawals.isEmpty())
    }

    @Test
    fun `PT23_M confirmation withdraws and goes to PT24`() = runTest {
        withdraw.onWithdrawClick()

        withdraw.events.test {
            withdraw.onConfirmWithdraw()
            assertEquals(WithdrawConsentEvent.NavigateToWithdrawn, awaitItem())
        }
        assertEquals(1, careLinks.withdrawals.size)
        assertFalse(withdraw.state.value.showConfirmDialog)
        assertFalse(withdraw.state.value.isWithdrawing)
    }

    @Test
    fun `consent no longer in effect shows the notice and then goes to PT24 anyway`() = runTest {
        careLinks.withdrawResult = failureOf(DomainError.Forbidden("NoActiveConsent"))
        withdraw.onWithdrawClick()
        withdraw.onConfirmWithdraw()
        assertTrue(withdraw.state.value.showNoActiveConsent)

        withdraw.events.test {
            withdraw.onNoActiveConsentAcknowledged()
            assertEquals(WithdrawConsentEvent.NavigateToWithdrawn, awaitItem())
        }
    }

    @Test
    fun `server failure keeps the patient on PT23 with a retry`() {
        careLinks.withdrawResult = failureOf(DomainError.Unexpected("HTTP_500"))
        withdraw.onWithdrawClick()
        withdraw.onConfirmWithdraw()

        assertTrue(withdraw.state.value.showServerError)

        careLinks.withdrawResult = Result.success(Unit)
        withdraw.onServerErrorRetry()
        assertEquals(2, careLinks.withdrawals.size)
    }

    @Test
    fun `offline PT23 does not open the confirmation`() {
        connectivity.online.value = false

        withdraw.onWithdrawClick()

        assertFalse(withdraw.state.value.showConfirmDialog)
    }

    @Test
    fun `PT2_1 reviews the remembered pending link`() = runTest {
        careLinks.pending[PatientId(12)] = CareLinkId(8)

        pending.events.test {
            pending.onReviewConsent()
            assertEquals(PendingConsentEvent.NavigateToConsent(8), awaitItem())
        }
    }

    @Test
    fun `PT2_1 without a remembered link goes back to scanning`() = runTest {
        pending.events.test {
            pending.onReviewConsent()
            assertEquals(PendingConsentEvent.NavigateToScanInvitation, awaitItem())
        }
    }

    @Test
    fun `PT2_1 signs out`() = runTest {
        pending.events.test {
            pending.onSignOut()
            assertEquals(PendingConsentEvent.SignedOut, awaitItem())
        }
        assertEquals(1, session.logoutCalls)
    }
}
