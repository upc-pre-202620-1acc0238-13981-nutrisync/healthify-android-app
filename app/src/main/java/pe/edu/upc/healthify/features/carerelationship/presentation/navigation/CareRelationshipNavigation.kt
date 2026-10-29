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
 * Navigation callbacks for the scan, consent, pending consent, switch nutritionist, withdraw and withdrawn screens;
 * the root graph decides how to build the back stack. Every step that changes the care link state (redeem, consent,
 * withdraw) **clears the back stack**: "back" never returns to a screen that no longer matches the care link.
 */
data class CareRelationshipNavigationCallbacks(
    val onBack: () -> Unit,
    /** Scan → consent after redeeming (clears). */
    val onInvitationRedeemed: (careLinkId: Long) -> Unit,
    /** Pending consent → consent (pushed). */
    val onReviewConsent: (careLinkId: Long) -> Unit,
    /** Consent "Not now" → pending consent (clears). */
    val onConsentPostponed: () -> Unit,
    /** Consent → home, the patient shell (clears). */
    val onConsentGranted: () -> Unit,
    /** → Scan with no care link (clears): link closed at consent, none remembered while pending, or withdrawal "Done". */
    val onScanNewInvitation: () -> Unit,
    /** Switch nutritionist "Scan invitation" → scan with `replaceActiveLink = true`, replacing the dialog. */
    val onSwitchPractitionerConfirmed: () -> Unit,
    /** Withdraw → withdrawn (clears). */
    val onConsentWithdrawn: () -> Unit,
    val onSignedOut: () -> Unit,
    val onExitApp: () -> Unit,
    /** "Yes, discharge" → roster with the discharge Snackbar (clears the patient file). */
    val onPatientDischarged: (patientName: String) -> Unit = {},
)

/** CareRelationship destinations in the root graph (patient, plus the nutritionist's invite and discharge screens). */
fun NavGraphBuilder.careRelationshipDestinations(callbacks: CareRelationshipNavigationCallbacks) {
    composable<ScanInvitationRoute> { entry ->
        // From the switch dialog it is pushed over the shell; from entry routing or after withdrawal it is the first screen.
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
