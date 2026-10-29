package pe.edu.upc.healthify.features.onboarding.presentation.state

/**
 * [showOfflineBanner]: design note "Offline: top banner on tapping a button"; hidden when the connection returns.
 */
data class WelcomeUiState(
    val showOfflineBanner: Boolean = false,
)

sealed interface WelcomeEvent {
    data object NavigateToSignUp : WelcomeEvent
    data object NavigateToSignIn : WelcomeEvent
}
