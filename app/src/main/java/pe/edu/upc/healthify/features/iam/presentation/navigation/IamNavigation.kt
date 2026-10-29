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

/** S3 · Registro. */
@Serializable
data object SignUpRoute

/** S4 · Login único. */
@Serializable
data object SignInRoute

/** IAM-2 · Recuperar contraseña (desde «¿Olvidaste tu contraseña?» de S4). */
@Serializable
data object ForgotPasswordRoute

/** S5 · Cargando (elige el shell). */
@Serializable
data object ShellLoadingRoute

/** S6 · Sesión expirada. */
@Serializable
data object SessionExpiredRoute

/** Callbacks de navegación de S3–S6; el grafo raíz decide cómo se arma el back stack. */
data class IamNavigationCallbacks(
    val onSignedIn: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToSignUp: () -> Unit,
    /** S4 → IAM-2. */
    val onGoToForgotPassword: () -> Unit,
    /** IAM-2 → S4, al volver o con la contraseña ya cambiada. */
    val onForgotPasswordDone: () -> Unit,
    val onShellSelected: (ShellDestination) -> Unit,
    /** S6 → S4: la sesión anterior ya no sirve, así que S4 queda como única pantalla. */
    val onExpiredSessionSignIn: () -> Unit,
    val onNoSession: () -> Unit,
    val onCloseApp: () -> Unit,
)

/** Destinos de Iam (S3–S6) dentro del grafo de entrada. */
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
