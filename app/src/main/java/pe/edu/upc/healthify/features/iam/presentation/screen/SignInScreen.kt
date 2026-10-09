package pe.edu.upc.healthify.features.iam.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.presentation.components.AuthCardLayout
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.features.iam.presentation.state.SignInError
import pe.edu.upc.healthify.features.iam.presentation.state.SignInEvent
import pe.edu.upc.healthify.features.iam.presentation.state.SignInUiState
import pe.edu.upc.healthify.features.iam.presentation.viewmodel.SignInViewModel

// Medidas del frame «S4 · Login único».
private val LogoSize = DpSize(56.dp, 50.dp)
private val GreenShapeOffsetY = 75.dp

/** Acciones de S4 que la pantalla con estado conecta con el ViewModel. */
data class SignInActions(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
    val onForgotPasswordDismiss: () -> Unit = {},
    val onCreateAccount: () -> Unit = {},
    val onErrorDialogRetry: () -> Unit = {},
    val onErrorDialogDismiss: () -> Unit = {},
)

/** S4 · Login único (S4.1 credenciales incorrectas · S4.2 cuenta bloqueada · S4.3 teclado). */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SignInEvent.NavigateToShellLoading -> onSignedIn()
            SignInEvent.NavigateToSignUp -> onCreateAccount()
        }
    }
    SignInContent(
        state = state,
        actions = SignInActions(
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onSubmit = viewModel::onSubmit,
            onForgotPassword = viewModel::onForgotPasswordClick,
            onForgotPasswordDismiss = viewModel::onForgotPasswordDismiss,
            onCreateAccount = viewModel::onCreateAccountClick,
            onErrorDialogRetry = viewModel::onErrorDialogRetry,
            onErrorDialogDismiss = viewModel::onErrorDialogDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun SignInContent(
    state: SignInUiState,
    actions: SignInActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onSubmit()
    }
    val submitButton: @Composable () -> Unit = {
        HealthifyButton(
            text = stringResource(if (state.isSubmitting) R.string.sign_in_submitting else R.string.sign_in_submit),
            onClick = submit,
            loading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val credentialsError = state.error == SignInError.InvalidCredentials

    AuthCardLayout(
        background = scheme.secondary,
        greenShapeOffsetY = GreenShapeOffsetY,
        keyboardAction = submitButton,
        modifier = modifier,
        header = { HealthifyLogo(variant = HealthifyLogoVariant.White, size = LogoSize) },
    ) { keyboardOpen ->
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        Text(
            text = stringResource(R.string.sign_in_title),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Text(
                text = stringResource(R.string.sign_in_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            if (state.error == SignInError.AccountLocked) AccountLockedNotice()
            HealthifyTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.auth_email),
                placeholder = stringResource(R.string.auth_email_placeholder),
                errorText = if (credentialsError) stringResource(R.string.sign_in_error_credentials) else null,
                type = HealthifyTextFieldType.Email,
                imeAction = ImeAction.Next,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HealthifyTextField(
            value = state.password,
            onValueChange = actions.onPasswordChange,
            label = stringResource(R.string.auth_password),
            placeholder = stringResource(R.string.sign_in_password_placeholder),
            // S4.1: el borde de la contraseña también va en error; el mensaje se muestra una sola vez (en el correo).
            isError = credentialsError,
            type = HealthifyTextFieldType.Password,
            imeAction = ImeAction.Go,
            keyboardActions = KeyboardActions(onGo = { submit() }),
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
            HealthifyButton(
                text = stringResource(R.string.sign_in_forgot_password),
                onClick = actions.onForgotPassword,
                style = HealthifyButtonStyle.Text,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!keyboardOpen) submitButton()
            HealthifyButton(
                text = stringResource(R.string.sign_in_create_account),
                onClick = actions.onCreateAccount,
                style = HealthifyButtonStyle.Text,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.showForgotPasswordDialog) {
        // DECISIÓN S4/IAM-2: «¿Olvidaste tu contraseña?» no tiene endpoint en el backend (IAM-2 pendiente).
        HealthifyDialog(
            title = stringResource(R.string.sign_in_forgot_dialog_title),
            text = stringResource(R.string.sign_in_forgot_dialog_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onForgotPasswordDismiss,
            onDismiss = actions.onForgotPasswordDismiss,
            dismissLabel = null,
        )
    }
    if (state.showErrorDialog) {
        ServerErrorDialog(onRetry = actions.onErrorDialogRetry, onDismiss = actions.onErrorDialogDismiss)
    }
}

/** «Aviso · cuenta bloqueada» de S4.2. */
@Composable
private fun AccountLockedNotice(modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimens.space8)
            .background(scheme.errorContainer, MaterialTheme.shapes.large)
            .padding(dimens.space16)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(dimens.space4),
    ) {
        Text(
            text = stringResource(R.string.sign_in_locked_title),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onErrorContainer,
        )
        Text(
            text = stringResource(R.string.sign_in_locked_body),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onErrorContainer,
        )
    }
}

@Preview(name = "S4 · Login único", widthDp = 360, heightDp = 800)
@Composable
private fun SignInPreview() {
    HealthifyTheme { SignInContent(state = SignInUiState(), actions = SignInActions()) }
}

@Preview(name = "S4.1 · Login único — credenciales incorrectas", widthDp = 360, heightDp = 800)
@Composable
private fun SignInInvalidCredentialsPreview() {
    HealthifyTheme {
        SignInContent(
            state = SignInUiState(
                email = "maria.flores@correo.com",
                password = "secreto12",
                error = SignInError.InvalidCredentials,
            ),
            actions = SignInActions(),
        )
    }
}

@Preview(name = "S4.2 · Login único — cuenta bloqueada", widthDp = 360, heightDp = 800)
@Composable
private fun SignInLockedPreview() {
    HealthifyTheme { SignInContent(state = SignInUiState(error = SignInError.AccountLocked), actions = SignInActions()) }
}

@Preview(name = "S4 · Login único — cargando", widthDp = 360, heightDp = 800)
@Composable
private fun SignInLoadingPreview() {
    HealthifyTheme {
        SignInContent(
            state = SignInUiState(email = "maria.flores@correo.com", password = "secreto12", isSubmitting = true),
            actions = SignInActions(),
        )
    }
}

@Preview(name = "S4 · Login único — sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun SignInOfflinePreview() {
    HealthifyTheme { SignInContent(state = SignInUiState(isOffline = true), actions = SignInActions()) }
}

@Preview(name = "S4 · ¿Olvidaste tu contraseña? — Próximamente", widthDp = 360, heightDp = 800)
@Composable
private fun SignInForgotPasswordPreview() {
    HealthifyTheme {
        SignInContent(state = SignInUiState(showForgotPasswordDialog = true), actions = SignInActions())
    }
}
