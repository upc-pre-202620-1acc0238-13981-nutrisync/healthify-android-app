package pe.edu.upc.healthify.features.iam.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.iam.presentation.screen.ForgotPasswordScreen
import pe.edu.upc.healthify.features.iam.presentation.screen.SessionExpiredScreen
import pe.edu.upc.healthify.features.iam.presentation.screen.ShellLoadingScreen
import pe.edu.upc.healthify.features.iam.presentation.screen.SignInScreen
import pe.edu.upc.healthify.features.iam.presentation.screen.SignUpScreen
import pe.edu.upc.healthify.features.iam.presentation.state.ShellDestination

/** Sign-up. */
@Serializable
data object SignUpRoute

/** Single sign-in. */
@Serializable
data object SignInRoute

/** Password recovery (from "Forgot your password?" on sign-in). */
@Serializable
data object ForgotPasswordRoute

/** Loading (picks the shell). */
@Serializable
data object ShellLoadingRoute

/** Session expired. */
@Serializable
data object SessionExpiredRoute

/** Navigation callbacks for the Iam screens; the root graph decides how the back stack is built. */
data class IamNavigationCallbacks(
    val onSignedIn: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToSignUp: () -> Unit,
    /** Sign-in → password recovery. */
    val onGoToForgotPassword: () -> Unit,
    /** Password recovery → sign-in, on back or once the password has changed. */
    val onForgotPasswordDone: () -> Unit,
    val onShellSelected: (ShellDestination) -> Unit,
    /** Session expired → sign-in: the old session is no longer valid, so sign-in stays as the only screen. */
    val onExpiredSessionSignIn: () -> Unit,
    val onNoSession: () -> Unit,
    val onCloseApp: () -> Unit,
)

/** Iam destinations inside the entry graph. */
fun NavGraphBuilder.iamDestinations(callbacks: IamNavigationCallbacks) {
    composable<SignUpRoute> {
        SignUpScreen(onSignedIn = callbacks.onSignedIn, onSignIn = callbacks.onGoToSignIn)
    }
    composable<SignInRoute> {
        SignInScreen(
            onSignedIn = callbacks.onSignedIn,
            onCreateAccount = callbacks.onGoToSignUp,
            onForgotPassword = callbacks.onGoToForgotPassword,
        )
    }
    composable<ForgotPasswordRoute> { ForgotPasswordScreen(onGoToSignIn = callbacks.onForgotPasswordDone) }
    composable<ShellLoadingRoute> {
        ShellLoadingScreen(
            onNavigate = callbacks.onShellSelected,
            onNoSession = callbacks.onNoSession,
            onClose = callbacks.onCloseApp,
        )
    }
    composable<SessionExpiredRoute> { SessionExpiredScreen(onSignIn = callbacks.onExpiredSessionSignIn) }
}
