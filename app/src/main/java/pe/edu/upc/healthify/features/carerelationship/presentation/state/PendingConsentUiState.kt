package pe.edu.upc.healthify.features.carerelationship.presentation.state

/** Care link pending consent. */
data class PendingConsentUiState(
    val isSigningOut: Boolean = false,
)

sealed interface PendingConsentEvent {
    data class NavigateToConsent(val careLinkId: Long) : PendingConsentEvent

    /** The device no longer remembers the pending care link: the patient has to scan again. */
    data object NavigateToScanInvitation : PendingConsentEvent
    data object SignedOut : PendingConsentEvent
}
