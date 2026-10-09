package pe.edu.upc.healthify.features.carerelationship.presentation.state

/** PT2.1 · Vínculo pendiente de consentimiento. */
data class PendingConsentUiState(
    val isSigningOut: Boolean = false,
)

sealed interface PendingConsentEvent {
    data class NavigateToConsent(val careLinkId: Long) : PendingConsentEvent

    /** El dispositivo ya no recuerda el vínculo pendiente: hay que escanear de nuevo (PT1). */
    data object NavigateToScanInvitation : PendingConsentEvent
    data object SignedOut : PendingConsentEvent
}
