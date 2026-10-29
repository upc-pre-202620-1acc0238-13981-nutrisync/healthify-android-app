package pe.edu.upc.healthify.features.carerelationship.presentation.state

/**
 * Withdraw consent.
 *
 * @param showConfirmDialog final confirmation; stays open while withdrawing ("Withdrawing…").
 * @param showNoActiveConsent "Your consent is no longer current." (`NoActiveConsent`); closing it moves on to the
 *   withdrawn screen.
 */
data class WithdrawConsentUiState(
    val showConfirmDialog: Boolean = false,
    val isWithdrawing: Boolean = false,
    val isOffline: Boolean = false,
    val showNoActiveConsent: Boolean = false,
    val showServerError: Boolean = false,
)

sealed interface WithdrawConsentEvent {
    data object NavigateToWithdrawn : WithdrawConsentEvent
}
