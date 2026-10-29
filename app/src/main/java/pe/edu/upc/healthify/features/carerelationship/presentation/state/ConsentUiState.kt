package pe.edu.upc.healthify.features.carerelationship.presentation.state

import androidx.annotation.StringRes
import pe.edu.upc.healthify.R

/** Where the button of a consent error notice leads. */
enum class ConsentErrorAction { Dismiss, GoHome, ScanInvitation }

/** Consent screen validations (design "Notes" frame) by the consent request's `extensions.code`. */
enum class ConsentError(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    @param:StringRes val actionRes: Int,
    val action: ConsentErrorAction,
) {
    /** `ConsentScopeRequired` (400). Should not happen: the scope is fixed (`ConsentScope.CURRENT`). */
    ScopeRequired(
        R.string.consent_error_scope_title,
        R.string.consent_error_scope,
        R.string.common_understood,
        ConsentErrorAction.Dismiss,
    ),

    /** `DischargedLinkCannotBeReactivated` (409). */
    LinkDischarged(
        R.string.consent_error_closed_title,
        R.string.consent_error_discharged,
        R.string.consent_error_scan_action,
        ConsentErrorAction.ScanInvitation,
    ),

    /** `CareLinkAlreadyRevoked` (409). */
    LinkRevoked(
        R.string.consent_error_closed_title,
        R.string.consent_error_revoked,
        R.string.consent_error_scan_action,
        ConsentErrorAction.ScanInvitation,
    ),

    /** `ConsentAlreadyGranted` (409): the care link is already active. */
    AlreadyGranted(
        R.string.consent_error_already_title,
        R.string.consent_error_already_granted,
        R.string.consent_error_home_action,
        ConsentErrorAction.GoHome,
    ),
}

/**
 * Consent and scope.
 *
 * @param aiProcessingGranted the "Use AI features" switch. It starts **off** even though the
 *   design draws it on; AI is a separate consent and must be an explicit act by the patient (Ley 29733;
 *   the backend also assumes `false` if missing).
 * @param showServerError "Care link does not exist → server error" and network/5xx failures: Retry/Close dialog.
 */
data class ConsentUiState(
    val aiProcessingGranted: Boolean = false,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
    val error: ConsentError? = null,
    val showServerError: Boolean = false,
)

sealed interface ConsentEvent {
    data object NavigateToHome : ConsentEvent
    data object NavigateToPendingConsent : ConsentEvent
    data object NavigateToScanInvitation : ConsentEvent
}
