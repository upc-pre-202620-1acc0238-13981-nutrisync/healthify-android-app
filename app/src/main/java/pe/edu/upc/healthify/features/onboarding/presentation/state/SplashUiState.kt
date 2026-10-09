package pe.edu.upc.healthify.features.onboarding.presentation.state

/** Adónde sigue la app después de S1. */
enum class SplashDestination { Welcome, ShellLoading, SessionExpired }

/** S1 tiene un solo visual; [destination] queda en `null` mientras se lee la sesión local. */
data class SplashUiState(
    val destination: SplashDestination? = null,
)

sealed interface SplashEvent {
    data class Navigate(val destination: SplashDestination) : SplashEvent
}
