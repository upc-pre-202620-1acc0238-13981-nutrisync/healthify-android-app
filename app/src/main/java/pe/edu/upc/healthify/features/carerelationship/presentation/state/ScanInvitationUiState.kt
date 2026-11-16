package pe.edu.upc.healthify.features.carerelationship.presentation.state

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationCodeInput

/** Scan screen validations (design "Notes" frame), by the redeem `extensions.code`. No message blames the patient. */
enum class ScanInvitationError(@param:StringRes val messageRes: Int) {
    /** `InvitationNotValid` (400) or a QR code that does not look like an invitation. */
    InvalidFormat(R.string.scan_invitation_error_invalid),

    /** `InvitationNotFound` (404). */
    NotFound(R.string.scan_invitation_error_not_found),

    /** `InvitationAlreadyRedeemed` (409). */
    AlreadyUsed(R.string.scan_invitation_error_used),

    /** `InvitationExpired` (422). */
    Expired(R.string.scan_invitation_error_expired),

    /** `PatientCannotSelfLink` (403). */
    SelfLink(R.string.scan_invitation_error_self_link),

    /** `PatientAlreadyHasActiveLink` (409): scanned without going through the switch nutritionist dialog. */
    AlreadyLinked(R.string.scan_invitation_error_already_linked),

    /** `AlreadyLinkedToThisPractitioner` (409): from the switch dialog, the code belongs to the same nutritionist. */
    SamePractitioner(R.string.scan_invitation_error_same_practitioner),

    /** Offline when sending. */
    Network(R.string.error_network),

    /** Any other response (5xx, 403 without a code…). */
    Unexpected(R.string.ds_error_state_body),
}

/** Camera permission: not resolved yet, granted or denied. */
enum class CameraPermissionState { Unknown, Granted, Denied }

/**
 * Scan invitation (plus the camera permission prompt).
 *
 * @param isRedeeming "Looking for the code…": the QR code is being redeemed and scanning is paused.
 * @param error message of the last rejected scan; cleared when a different code is read.
 * @param showManualSheet the sheet to type the code by hand ("Type the code by hand", under the viewfinder). The camera
 *   pauses while it is open.
 * @param manualCode what was typed in the sheet. A secret: it is never logged and cleared when the sheet closes.
 * @param manualError message of the last rejected typed code; cleared as soon as the code is edited.
 * @param showSignOutDialog when this is the first screen (entry routing, after withdrawal), "back" offers to sign out
 *   with the sign-out modal; otherwise someone without an invitation could not leave their account.
 */
data class ScanInvitationUiState(
    val replaceActiveLink: Boolean = false,
    val cameraPermission: CameraPermissionState = CameraPermissionState.Unknown,
    val isRedeeming: Boolean = false,
    val error: ScanInvitationError? = null,
    val isOffline: Boolean = false,
    val showManualSheet: Boolean = false,
    val manualCode: ManualCode = ManualCode(),
    val manualError: ScanInvitationError? = null,
    val showSignOutDialog: Boolean = false,
    val isSigningOut: Boolean = false,
) {
    /** The camera analyzes only with permission, a connection, no redeem in progress and the typing sheet closed. */
    val isScanning: Boolean
        get() = cameraPermission == CameraPermissionState.Granted && !isRedeeming && !isOffline && !showManualSheet

    /** The typed code judged as it is typed: drives the helper text and whether "Link up" is enabled. */
    val manualInput: InvitationCodeInput
        get() = InvitationCodeInput.evaluate(manualCode.text)

    /** "Link up" works with a valid code, a connection and nothing already in flight. */
    val canSubmitManual: Boolean
        get() = manualInput is InvitationCodeInput.Valid && !isRedeeming && !isOffline
}

/** The text typed in the sheet. A secret like the token: [toString] hides it so a log of the state never leaks it. */
data class ManualCode(val text: String = "") {
    override fun toString(): String = "ManualCode(***)"
}

sealed interface ScanInvitationEvent {
    data class NavigateToConsent(val careLinkId: Long) : ScanInvitationEvent
    data object SignedOut : ScanInvitationEvent
}
