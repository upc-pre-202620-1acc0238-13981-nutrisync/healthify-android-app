package pe.edu.upc.healthify.features.carerelationship.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Scan invitation.
 *
 * @param replaceActiveLink `true` only when coming from the switch nutritionist confirmation: redeeming revokes the
 *   current care link. From entry routing or after withdrawal (patient with no care link) it is `false`.
 */
@Serializable
data class ScanInvitationRoute(val replaceActiveLink: Boolean = false) {
    companion object {
        /** Argument key in the ViewModel's `SavedStateHandle` (the property name). */
        const val ARG_REPLACE_ACTIVE_LINK = "replaceActiveLink"
    }
}

/**
 * Consent and scope for care link [careLinkId].
 *
 * @param fromPendingConsent `true` if opened from pending consent (back returns there); `false` if coming from
 *   redeeming on the scan screen, which makes no sense to show again: back goes to pending consent, like "Not now".
 */
@Serializable
data class ConsentRoute(val careLinkId: Long, val fromPendingConsent: Boolean = false) {
    companion object {
        const val ARG_CARE_LINK_ID = "careLinkId"
    }
}

/** Care link pending consent. */
@Serializable
data object PendingConsentRoute

/** "Switch nutritionist?" (dialog over Settings). */
@Serializable
data object SwitchPractitionerRoute

/** Withdraw consent (plus the final confirmation). */
@Serializable
data object WithdrawConsentRoute

/** Consent withdrawn. */
@Serializable
data object ConsentWithdrawnRoute

/** Settings - AI features. */
@Serializable
data object AiFeaturesRoute

/** Generate invitation (full screen over the nutritionist shell). */
@Serializable
data object InvitePatientRoute

/** Clinical discharge (from "Discharge" in the patient file). */
@Serializable
data class DischargePatientRoute(val careLinkId: Long, val patientName: String) {
    companion object {
        const val ARG_CARE_LINK_ID = "careLinkId"
        const val ARG_PATIENT_NAME = "patientName"
    }
}
