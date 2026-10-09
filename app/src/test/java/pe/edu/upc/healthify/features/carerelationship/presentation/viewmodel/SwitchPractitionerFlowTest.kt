package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GrantConsentUseCase
import pe.edu.upc.healthify.features.carerelationship.application.usecase.RedeemInvitationUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ConsentRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.ScanInvitationRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.ConsentEvent
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
 * Cambio de nutricionista (CR-1): PT21.V → PT1 con `replaceActiveLink = true` → PT2 sobre el vínculo nuevo.
 * Solo PT21.V manda el flag; desde S5 o PT24 va en `false`.
 */
class SwitchPractitionerFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val careLinks = FakeCareLinkRepository()
    private val session = FakeSessionRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun scanViewModel(route: ScanInvitationRoute) = ScanInvitationViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ScanInvitationRoute.ARG_REPLACE_ACTIVE_LINK to route.replaceActiveLink)),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        redeemInvitation = RedeemInvitationUseCase(careLinks),
        signOut = SignOutUseCase(FakeAuthenticationRepository(), session),
        connectivityObserver = connectivity,
    ).apply { onCameraPermissionResult(granted = true) }

    private fun consentViewModel(route: ConsentRoute) = ConsentViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ConsentRoute.ARG_CARE_LINK_ID to route.careLinkId)),
        observeCurrentUser = ObserveCurrentUserUseCase(session),
        grantConsent = GrantConsentUseCase(careLinks),
        connectivityObserver = connectivity,
    )

    @Test
    fun `switching practitioner replaces the active link and asks consent for the new one`() = runTest {
        careLinks.redeemResult = Result.success(careLink(id = 31))
        val scan = scanViewModel(ScanInvitationRoute(replaceActiveLink = true))

        var newLinkId = 0L
        scan.events.test {
            scan.onQrScanned(VALID_TOKEN)
            newLinkId = (awaitItem() as ScanInvitationEvent.NavigateToConsent).careLinkId
        }

        assertTrue(scan.state.value.replaceActiveLink)
        assertTrue(careLinks.redemptions.single().replaceActiveLink)
        assertEquals(31L, newLinkId)
        // El vínculo nuevo nace sin consentimiento: S5 lo trataría como pendiente (PT2.1) si se sale ahora.
        assertEquals(CareLinkId(31), careLinks.pending[PatientId(12)])

        val consent = consentViewModel(ConsentRoute(newLinkId))
        consent.onAiProcessingChange(true)
        consent.events.test {
            consent.onGrantConsent()
            assertEquals(ConsentEvent.NavigateToHome, awaitItem())
        }
        assertEquals(CareLinkId(31), careLinks.grants.single().careLinkId)
        assertTrue(careLinks.grants.single().aiProcessingGranted)
    }

    @Test
    fun `scanning from S5 never replaces a link`() {
        scanViewModel(ScanInvitationRoute()).onQrScanned(VALID_TOKEN)

        assertFalse(careLinks.redemptions.single().replaceActiveLink)
    }

    @Test
    fun `a code from the same practitioner says so and keeps the current link`() {
        careLinks.redeemResult = failureOf(DomainError.Conflict("AlreadyLinkedToThisPractitioner"))
        val scan = scanViewModel(ScanInvitationRoute(replaceActiveLink = true))

        scan.onQrScanned(VALID_TOKEN)

        assertEquals(ScanInvitationError.SamePractitioner, scan.state.value.error)
        assertTrue(careLinks.pending.isEmpty())
    }

    @Test
    fun `without PT21_V an existing link is reported instead of replaced`() {
        careLinks.redeemResult = failureOf(DomainError.Conflict("PatientAlreadyHasActiveLink"))
        val scan = scanViewModel(ScanInvitationRoute())

        scan.onQrScanned(VALID_TOKEN)

        assertEquals(ScanInvitationError.AlreadyLinked, scan.state.value.error)
    }
}
