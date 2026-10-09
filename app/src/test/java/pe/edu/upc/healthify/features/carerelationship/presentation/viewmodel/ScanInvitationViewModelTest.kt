package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.RedeemInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ScanInvitationRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.CameraPermissionState
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationError
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ScanInvitationEvent
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.SignOutUseCase
import pe.edu.upc.healthify.testing.FakeAuthenticationRepository
import pe.edu.upc.healthify.testing.FakeCareLinkRepository
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.VALID_TOKEN
import pe.edu.upc.healthify.testing.careLink
import pe.edu.upc.healthify.testing.failureOf

class ScanInvitationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakeCareLinkRepository()
    private val session = FakeSessionRepository()
    private val auth = FakeAuthenticationRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun viewModel(replaceActiveLink: Boolean = false) = ScanInvitationViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ScanInvitationRoute.ARG_REPLACE_ACTIVE_LINK to replaceActiveLink)),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        redeemInvitation = RedeemInvitationUseCase(careLinks),
        signOut = SignOutUseCase(auth, session),
        connectivityObserver = connectivity,
    ).apply { onCameraPermissionResult(granted = true) }

    @Test
    fun `a valid QR is redeemed and opens PT2 with the new link`() = runTest {
        careLinks.redeemResult = Result.success(careLink(id = 8))
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onQrScanned(VALID_TOKEN)
            assertEquals(ScanInvitationEvent.NavigateToConsent(8), awaitItem())
        }
        assertEquals(VALID_TOKEN, careLinks.redemptions.single().token.value)
        assertFalse(careLinks.redemptions.single().replaceActiveLink)
        assertEquals(CareLinkId(8), careLinks.pending[PatientId(12)])
        assertFalse(viewModel.state.value.isRedeeming)
    }

    @Test
    fun `each backend code shows its PT1 message`() {
        val expected = mapOf(
            DomainError.Validation("InvitationNotValid") to
                (ScanInvitationError.InvalidFormat to R.string.scan_invitation_error_invalid),
            DomainError.NotFound("InvitationNotFound") to
                (ScanInvitationError.NotFound to R.string.scan_invitation_error_not_found),
            DomainError.Conflict("InvitationAlreadyRedeemed") to
                (ScanInvitationError.AlreadyUsed to R.string.scan_invitation_error_used),
            DomainError.Validation("InvitationExpired") to
                (ScanInvitationError.Expired to R.string.scan_invitation_error_expired),
            DomainError.Forbidden("PatientCannotSelfLink") to
                (ScanInvitationError.SelfLink to R.string.scan_invitation_error_self_link),
            DomainError.Conflict("PatientAlreadyHasActiveLink") to
                (ScanInvitationError.AlreadyLinked to R.string.scan_invitation_error_already_linked),
            DomainError.Conflict("AlreadyLinkedToThisPractitioner") to
                (ScanInvitationError.SamePractitioner to R.string.scan_invitation_error_same_practitioner),
            DomainError.Network to (ScanInvitationError.Network to R.string.error_network),
            DomainError.Unexpected("HTTP_500") to (ScanInvitationError.Unexpected to R.string.ds_error_state_body),
            // 403 de rol sin cuerpo: no es auto-vínculo.
            DomainError.Forbidden(null) to (ScanInvitationError.Unexpected to R.string.ds_error_state_body),
        )
        expected.forEach { (error, pair) ->
            val (screenError, messageRes) = pair
            careLinks.redeemResult = failureOf(error)
            val viewModel = viewModel()

            viewModel.onQrScanned(VALID_TOKEN)

            assertEquals("$error", screenError, viewModel.state.value.error)
            assertEquals("$error", messageRes, viewModel.state.value.error?.messageRes)
            assertFalse(viewModel.state.value.isRedeeming)
        }
    }

    @Test
    fun `a QR that is not an invitation shows the invalid message without calling the backend`() {
        val viewModel = viewModel()

        viewModel.onQrScanned("WIFI:S:Consultorio;T:WPA;P:secreto;;")

        assertEquals(ScanInvitationError.InvalidFormat, viewModel.state.value.error)
        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `a rejected code is not sent again while it stays in front of the camera`() {
        careLinks.redeemResult = failureOf(DomainError.Validation("InvitationExpired"))
        val viewModel = viewModel()

        repeat(5) { viewModel.onQrScanned(VALID_TOKEN) }

        assertEquals(1, careLinks.redemptions.size)
        assertEquals(ScanInvitationError.Expired, viewModel.state.value.error)
    }

    @Test
    fun `a different code clears the previous message and is redeemed`() = runTest {
        careLinks.redeemResult = failureOf(DomainError.Validation("InvitationExpired"))
        val viewModel = viewModel()
        viewModel.onQrScanned(VALID_TOKEN)

        careLinks.redeemResult = Result.success(careLink(id = 9))
        viewModel.events.test {
            viewModel.onQrScanned(VALID_TOKEN.reversed())
            assertEquals(ScanInvitationEvent.NavigateToConsent(9), awaitItem())
        }
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `after a network failure the same code can be tried again`() {
        careLinks.redeemResult = failureOf(DomainError.Network)
        val viewModel = viewModel()

        viewModel.onQrScanned(VALID_TOKEN)
        viewModel.onQrScanned(VALID_TOKEN)

        assertEquals(2, careLinks.redemptions.size)
    }

    @Test
    fun `offline the camera does not redeem and the no-camera button is the only thing disabled`() {
        connectivity.online.value = false
        val viewModel = viewModel()

        viewModel.onQrScanned(VALID_TOKEN)

        assertTrue(viewModel.state.value.isOffline)
        assertFalse(viewModel.state.value.isScanning)
        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `without camera permission PT1_M is shown and nothing is scanned`() {
        val viewModel = viewModel()
        viewModel.onCameraPermissionResult(granted = false)

        viewModel.onQrScanned(VALID_TOKEN)

        assertEquals(CameraPermissionState.Denied, viewModel.state.value.cameraPermission)
        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `back at the first screen offers to sign out`() = runTest {
        val viewModel = viewModel()
        viewModel.onBackAtRoot()
        assertTrue(viewModel.state.value.showSignOutDialog)

        viewModel.events.test {
            viewModel.onSignOutConfirm()
            assertEquals(ScanInvitationEvent.SignedOut, awaitItem())
        }
        assertEquals(1, auth.signOutCalls)
        assertEquals(1, session.logoutCalls)
    }
}
