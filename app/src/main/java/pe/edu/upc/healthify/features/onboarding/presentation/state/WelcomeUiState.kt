package pe.edu.upc.healthify.features.onboarding.presentation.state

/**
 * S2. [showOfflineBanner]: nota «Sin conexión: banner superior al tocar un botón»; se oculta al volver la conexión.
 */
data class WelcomeUiState(
    val showOfflineBanner: Boolean = false,
)

sealed interface WelcomeEvent {
    data object NavigateToSignUp : WelcomeEvent
    data object NavigateToSignIn : WelcomeEvent
}
