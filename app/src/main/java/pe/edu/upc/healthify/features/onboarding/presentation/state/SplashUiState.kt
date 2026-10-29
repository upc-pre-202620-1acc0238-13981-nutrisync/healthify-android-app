package pe.edu.upc.healthify.features.onboarding.presentation.state

/** Where the app goes after the splash. */
enum class SplashDestination { Welcome, ShellLoading, SessionExpired }

/** The splash has a single visual; [destination] stays `null` while the local session is read. */
data class SplashUiState(
    val destination: SplashDestination? = null,
)

sealed interface SplashEvent {
    data class Navigate(val destination: SplashDestination) : SplashEvent
}
