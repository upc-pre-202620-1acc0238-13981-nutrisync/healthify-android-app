package pe.edu.upc.healthify.features.onboarding.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.onboarding.presentation.screen.SplashScreen
import pe.edu.upc.healthify.features.onboarding.presentation.screen.WelcomeScreen
import pe.edu.upc.healthify.features.onboarding.presentation.state.SplashDestination

/** Splash. */
@Serializable
data object SplashRoute

/** Welcome. */
@Serializable
data object WelcomeRoute

/** Onboarding destinations (splash, welcome) inside the entry graph. */
fun NavGraphBuilder.onboardingDestinations(
    onSplashResolved: (SplashDestination) -> Unit,
    onCreateAccount: () -> Unit,
    onHaveAccount: () -> Unit,
) {
    composable<SplashRoute> { SplashScreen(onNavigate = onSplashResolved) }
    composable<WelcomeRoute> { WelcomeScreen(onCreateAccount = onCreateAccount, onHaveAccount = onHaveAccount) }
}
