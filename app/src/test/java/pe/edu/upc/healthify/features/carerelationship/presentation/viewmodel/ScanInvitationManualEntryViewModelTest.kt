package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.RedeemInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationCodeInput
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
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

/**
 * Type the invitation code by hand (the sheet under the viewfinder): states, validation, the same redeem path and the
 * same error messages as the QR, and the guarantee that the QR flow keeps working.
 */
class ScanInvitationManualEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakeCareLinkRepository()
    private val session = FakeSessionRepository()
    private val auth = FakeAuthenticationRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun viewModel(
        replaceActiveLink: Boolean = false,
        repository: CareLinkRepository = careLinks,
    ) = ScanInvitationViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ScanInvitationRoute.ARG_REPLACE_ACTIVE_LINK to replaceActiveLink)),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        redeemInvitation = RedeemInvitationUseCase(repository),
        signOut = SignOutUseCase(auth, session),
        connectivityObserver = connectivity,
    ).apply { onCameraPermissionResult(granted = true) }

    private fun ScanInvitationViewModel.type(text: String) = onManualCodeChange(text)

    // ------------------------------------------------------------------ the sheet

    @Test
    fun `the sheet is closed at first and the camera is scanning`() {
        val state = viewModel().state.value

        assertFalse(state.showManualSheet)
        assertTrue(state.isScanning)
        assertEquals("", state.manualCode.text)
        assertEquals(InvitationCodeInput.Empty, state.manualInput)
    }

    @Test
    fun `opening the sheet pauses the camera and closing it resumes it`() {
        val viewModel = viewModel()

        viewModel.onManualEntryClick()
        assertTrue(viewModel.state.value.showManualSheet)
        assertFalse(viewModel.state.value.isScanning)

        viewModel.onManualDismiss()
        assertFalse(viewModel.state.value.showManualSheet)
        assertTrue(viewModel.state.value.isScanning)
    }

    @Test
    fun `opening the sheet clears a QR rejection that was on screen`() {
        careLinks.redeemResult = failureOf(DomainError.Validation("InvitationExpired"))
        val viewModel = viewModel()
        viewModel.onQrScanned(VALID_TOKEN)
        assertEquals(ScanInvitationError.Expired, viewModel.state.value.error)

        viewModel.onManualEntryClick()

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `closing the sheet forgets what was typed`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualDismiss()
        viewModel.onManualEntryClick()

        assertEquals("", viewModel.state.value.manualCode.text)
    }

    @Test
    fun `the typed code never appears when the state is printed`() {
        val viewModel = viewModel()
        viewModel.type(VALID_TOKEN)

        assertFalse(viewModel.state.value.toString().contains(VALID_TOKEN))
        assertFalse(viewModel.state.value.toString().contains(VALID_TOKEN.take(8)))
    }

    // ------------------------------------------------------------------ typing and validation

    @Test
    fun `Link up needs a valid code`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()

        assertFalse(viewModel.state.value.canSubmitManual)
        viewModel.type("kP3x 9Qm2")
        assertEquals(InvitationCodeInput.TooShort(8), viewModel.state.value.manualInput)
        assertFalse(viewModel.state.value.canSubmitManual)
        viewModel.type("kP3x9Qm2LrT7vB8nZc4WdA!")
        assertEquals(InvitationCodeInput.InvalidCharacters, viewModel.state.value.manualInput)
        assertFalse(viewModel.state.value.canSubmitManual)
        viewModel.type(VALID_TOKEN)
        assertTrue(viewModel.state.value.canSubmitManual)
    }

    @Test
    fun `submitting a code that cannot be a token never calls the backend`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()

        listOf("", "   ", "abc", "kP3x9Qm2LrT7vB8nZc4WdA!", "a".repeat(80)).forEach {
            viewModel.type(it)
            viewModel.onManualSubmit()
        }

        assertTrue(careLinks.redemptions.isEmpty())
        assertFalse(viewModel.state.value.isRedeeming)
    }

    @Test
    fun `editing the code clears the previous rejection`() {
        careLinks.redeemResult = failureOf(DomainError.Conflict("InvitationAlreadyRedeemed"))
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)
        viewModel.onManualSubmit()
        assertEquals(ScanInvitationError.AlreadyUsed, viewModel.state.value.manualError)

        viewModel.type(VALID_TOKEN.dropLast(1))

        assertNull(viewModel.state.value.manualError)
    }

    @Test
    fun `an absurdly long paste is cut instead of kept`() {
        val viewModel = viewModel()

        viewModel.type("a".repeat(5_000))

        assertEquals(128, viewModel.state.value.manualCode.text.length)
        assertEquals(InvitationCodeInput.TooLong, viewModel.state.value.manualInput)
    }

    // ------------------------------------------------------------------ success

    @Test
    fun `a valid code is redeemed through the same use case and opens the consent step`() = runTest {
        careLinks.redeemResult = Result.success(careLink(id = 8))
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.events.test {
            viewModel.onManualSubmit()
            assertEquals(ScanInvitationEvent.NavigateToConsent(8), awaitItem())
        }

        assertEquals(VALID_TOKEN, careLinks.redemptions.single().token.value)
        assertFalse(careLinks.redemptions.single().replaceActiveLink)
        // The device remembers the pending link, exactly as after a scan.
        assertEquals(CareLinkId(8), careLinks.pending[PatientId(12)])
        val state = viewModel.state.value
        assertFalse(state.isRedeeming)
        assertFalse(state.showManualSheet)
        assertEquals("", state.manualCode.text)
        assertNull(state.manualError)
    }

    @Test
    fun `spaces in the typed code are removed before it is sent`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(" ${VALID_TOKEN.chunked(4).joinToString(" ")} \n")

        viewModel.onManualSubmit()

        assertEquals(VALID_TOKEN, careLinks.redemptions.single().token.value)
    }

    @Test
    fun `upper and lower case are sent exactly as typed`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type("AbCdEfGhIjKlMnOpQrStUv")

        viewModel.onManualSubmit()

        assertEquals("AbCdEfGhIjKlMnOpQrStUv", careLinks.redemptions.single().token.value)
    }

    @Test
    fun `switching nutritionist passes the confirmed replace flag like the QR does`() {
        val viewModel = viewModel(replaceActiveLink = true)
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualSubmit()

        assertTrue(careLinks.redemptions.single().replaceActiveLink)
    }

    // ------------------------------------------------------------------ errors

    @Test
    fun `each backend code shows its message inside the sheet and keeps the typed code`() {
        val expected = mapOf(
            DomainError.Validation("InvitationNotValid") to ScanInvitationError.InvalidFormat,
            DomainError.NotFound("InvitationNotFound") to ScanInvitationError.NotFound,
            DomainError.Conflict("InvitationAlreadyRedeemed") to ScanInvitationError.AlreadyUsed,
            DomainError.Validation("InvitationExpired") to ScanInvitationError.Expired,
            DomainError.Forbidden("PatientCannotSelfLink") to ScanInvitationError.SelfLink,
            DomainError.Conflict("PatientAlreadyHasActiveLink") to ScanInvitationError.AlreadyLinked,
            DomainError.Conflict("AlreadyLinkedToThisPractitioner") to ScanInvitationError.SamePractitioner,
            DomainError.Network to ScanInvitationError.Network,
            DomainError.Unexpected("HTTP_500") to ScanInvitationError.Unexpected,
        )
        expected.forEach { (error, screenError) ->
            careLinks.redeemResult = failureOf(error)
            val viewModel = viewModel()
            viewModel.onManualEntryClick()
            viewModel.type(VALID_TOKEN)

            viewModel.onManualSubmit()

            val state = viewModel.state.value
            assertEquals("$error", screenError, state.manualError)
            assertTrue("$error", state.showManualSheet)
            assertEquals("$error", VALID_TOKEN, state.manualCode.text)
            assertFalse("$error", state.isRedeeming)
            // The QR message line stays empty: the error belongs to the sheet.
            assertNull("$error", state.error)
        }
    }

    @Test
    fun `after a network failure the same code can be sent again`() {
        careLinks.redeemResult = failureOf(DomainError.Network)
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualSubmit()
        viewModel.onManualSubmit()

        assertEquals(2, careLinks.redemptions.size)
    }

    @Test
    fun `the code that failed can be fixed and sent again`() = runTest {
        careLinks.redeemResult = failureOf(DomainError.Validation("InvitationNotValid"))
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN.reversed())
        viewModel.onManualSubmit()
        assertEquals(ScanInvitationError.InvalidFormat, viewModel.state.value.manualError)

        careLinks.redeemResult = Result.success(careLink(id = 9))
        viewModel.type(VALID_TOKEN)
        viewModel.events.test {
            viewModel.onManualSubmit()
            assertEquals(ScanInvitationEvent.NavigateToConsent(9), awaitItem())
        }
        assertEquals(2, careLinks.redemptions.size)
    }

    // ------------------------------------------------------------------ loading, offline, session

    @Test
    fun `while redeeming the code is locked and a second submit or a dismiss do nothing`() = runTest {
        val gate = CompletableDeferred<Result<CareLink>>()
        val slow = object : CareLinkRepository by careLinks {
            override suspend fun redeemInvitation(token: InvitationToken, replaceActiveLink: Boolean): Result<CareLink> {
                careLinks.redemptions += FakeCareLinkRepository.Redemption(token, replaceActiveLink)
                return gate.await()
            }
        }
        val viewModel = viewModel(repository = slow)
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualSubmit()
        assertTrue(viewModel.state.value.isRedeeming)
        viewModel.onManualSubmit()
        viewModel.type("other")
        viewModel.onManualDismiss()

        assertEquals(1, careLinks.redemptions.size)
        assertTrue(viewModel.state.value.showManualSheet)
        assertEquals(VALID_TOKEN, viewModel.state.value.manualCode.text)
        assertFalse(viewModel.state.value.canSubmitManual)

        viewModel.events.test {
            gate.complete(Result.success(careLink(id = 5)))
            assertEquals(ScanInvitationEvent.NavigateToConsent(5), awaitItem())
        }
        assertFalse(viewModel.state.value.showManualSheet)
    }

    @Test
    fun `offline the code is not sent and Link up is disabled`() {
        connectivity.online.value = false
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualSubmit()

        assertTrue(viewModel.state.value.isOffline)
        assertFalse(viewModel.state.value.canSubmitManual)
        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `coming back online enables Link up again`() {
        connectivity.online.value = false
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)
        assertFalse(viewModel.state.value.canSubmitManual)

        connectivity.online.value = true

        assertTrue(viewModel.state.value.canSubmitManual)
    }

    @Test
    fun `if the session ended while typing nothing is sent and the loading stops`() {
        session.user.value = null
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        viewModel.onManualSubmit()

        assertTrue(careLinks.redemptions.isEmpty())
        assertFalse(viewModel.state.value.isRedeeming)
    }

    // ------------------------------------------------------------------ the QR flow is untouched

    @Test
    fun `the camera ignores a QR while the sheet is open`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()

        viewModel.onQrScanned(VALID_TOKEN)

        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `after closing the sheet a QR is redeemed as before`() = runTest {
        careLinks.redeemResult = Result.success(careLink(id = 8))
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)
        viewModel.onManualDismiss()

        viewModel.events.test {
            viewModel.onQrScanned(VALID_TOKEN)
            assertEquals(ScanInvitationEvent.NavigateToConsent(8), awaitItem())
        }
        assertEquals(VALID_TOKEN, careLinks.redemptions.single().token.value)
    }

    @Test
    fun `a QR that is not an invitation still shows the invalid message on the scan screen`() {
        val viewModel = viewModel()

        viewModel.onQrScanned("WIFI:S:Consultorio;T:WPA;P:secreto;;")

        assertEquals(ScanInvitationError.InvalidFormat, viewModel.state.value.error)
        assertNull(viewModel.state.value.manualError)
        assertTrue(careLinks.redemptions.isEmpty())
    }

    @Test
    fun `the camera permission state is not touched by typing`() {
        val viewModel = viewModel()
        viewModel.onManualEntryClick()
        viewModel.type(VALID_TOKEN)

        assertEquals(CameraPermissionState.Granted, viewModel.state.value.cameraPermission)
    }
}
