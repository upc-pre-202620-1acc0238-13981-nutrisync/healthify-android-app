package pe.edu.upc.healthify.features.carerelationship.presentation.state

/**
 * PT23 · Retirar consentimiento.
 *
 * @param showConfirmDialog PT23.M · confirmación final; sigue abierto mientras se retira («Retirando…»).
 * @param showNoActiveConsent «Tu consentimiento ya no está vigente.» (`NoActiveConsent`); al cerrarlo sigue a PT24.
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
