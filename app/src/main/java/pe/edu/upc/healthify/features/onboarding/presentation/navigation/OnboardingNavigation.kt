package pe.edu.upc.healthify.features.onboarding.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.onboarding.presentation.screen.SplashScreen
import pe.edu.upc.healthify.features.onboarding.presentation.screen.WelcomeScreen
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination

/** S1 · Splash. */
@Serializable
data object SplashRoute

/** S2 · Bienvenida. */
@Serializable
data object WelcomeRoute

/** Destinos de onboarding (S1, S2) dentro del grafo de entrada. */
fun NavGraphBuilder.onboardingDestinations(
    onSplashResolved: (SplashDestination) -> Unit,
    onCreateAccount: () -> Unit,
    onHaveAccount: () -> Unit,
) {
    composable<SplashRoute> { SplashScreen(onNavigate = onSplashResolved) }
    composable<WelcomeRoute> { WelcomeScreen(onCreateAccount = onCreateAccount, onHaveAccount = onHaveAccount) }
}
