package pe.edu.upc.healthify.features.carerelationship.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogProperties
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialogScrim
import pe.edu.upc.healthify.features.carerelationship.presentation.components.SwitchPractitionerDialogContent
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.AiFeaturesScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.DischargePatientScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.InvitePatientScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.ConsentScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.ConsentWithdrawnScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.PendingConsentScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.ScanInvitationScreen
import pe.edu.upc.healthify.features.carerelationship.presentation.screen.WithdrawConsentScreen

/**
 * Callbacks de navegación de PT1, PT2, PT2.1, PT21.V, PT23 y PT24; el grafo raíz decide cómo se arma el back stack.
 * Cada paso que cambia el estado del vínculo (canjear, consentir, retirar) **limpia el back stack**: con «atrás»
 * nunca se vuelve a una pantalla que ya no corresponde al vínculo.
 */
data class CareRelationshipNavigationCallbacks(
    val onBack: () -> Unit,
    /** PT1 → PT2 tras canjear (limpia). */
    val onInvitationRedeemed: (careLinkId: Long) -> Unit,
    /** PT2.1 → PT2 (se apila). */
    val onReviewConsent: (careLinkId: Long) -> Unit,
    /** PT2 «Ahora no» → PT2.1 (limpia). */
    val onConsentPostponed: () -> Unit,
    /** PT2 → PT3, el shell del paciente (limpia). */
    val onConsentGranted: () -> Unit,
    /** → PT1 sin vínculo (limpia): vínculo cerrado en PT2, PT2.1 sin vínculo recordado, PT24 «Listo». */
    val onScanNewInvitation: () -> Unit,
    /** PT21.V «Escanear invitación» → PT1 con `replaceActiveLink = true`, en lugar del diálogo. */
    val onSwitchPractitionerConfirmed: () -> Unit,
    /** PT23 → PT24 (limpia). */
    val onConsentWithdrawn: () -> Unit,
    val onSignedOut: () -> Unit,
    val onExitApp: () -> Unit,
    /** PR18 «Sí, dar de alta» → PR1 con el Snackbar del alta (limpia la ficha del paciente). */
    val onPatientDischarged: (patientName: String) -> Unit = {},
)

/** Destinos de CareRelationship en el grafo raíz (paciente, y PR2 y PR18 del nutricionista). */
fun NavGraphBuilder.careRelationshipDestinations(callbacks: CareRelationshipNavigationCallbacks) {
    composable<ScanInvitationRoute> { entry ->
        // Desde PT21.V se apila sobre el shell; desde S5 o PT24 es la primera pantalla.
        val replaceActiveLink = entry.toRoute<ScanInvitationRoute>().replaceActiveLink
        ScanInvitationScreen(
            onConsentRequired = callbacks.onInvitationRedeemed,
            onSignedOut = callbacks.onSignedOut,
            onBack = if (replaceActiveLink) callbacks.onBack else null,
            onExitApp = callbacks.onExitApp,
        )
    }
    composable<ConsentRoute> { entry ->
        val fromPendingConsent = entry.toRoute<ConsentRoute>().fromPendingConsent
        ConsentScreen(
            onBack = if (fromPendingConsent) callbacks.onBack else callbacks.onConsentPostponed,
            onConsentGranted = callbacks.onConsentGranted,
            onNotNow = callbacks.onConsentPostponed,
            onScanInvitation = callbacks.onScanNewInvitation,
        )
    }
    composable<PendingConsentRoute> {
        PendingConsentScreen(
            onReviewConsent = callbacks.onReviewConsent,
            onScanInvitation = callbacks.onScanNewInvitation,
            onSignedOut = callbacks.onSignedOut,
        )
    }
    dialog<SwitchPractitionerRoute>(dialogProperties = HealthifyDialogProperties) {
        HealthifyDialogScrim()
        SwitchPractitionerDialogContent(
            onScanInvitation = callbacks.onSwitchPractitionerConfirmed,
            onCancel = callbacks.onBack,
        )
    }
    composable<WithdrawConsentRoute> {
        WithdrawConsentScreen(onBack = callbacks.onBack, onWithdrawn = callbacks.onConsentWithdrawn)
    }
    composable<ConsentWithdrawnRoute> { ConsentWithdrawnScreen(onDone = callbacks.onScanNewInvitation) }
    composable<AiFeaturesRoute> { AiFeaturesScreen(onBack = callbacks.onBack) }
    composable<InvitePatientRoute> { InvitePatientScreen(onBack = callbacks.onBack) }
    composable<DischargePatientRoute> {
        DischargePatientScreen(onBack = callbacks.onBack, onDischarged = callbacks.onPatientDischarged)
    }
}
