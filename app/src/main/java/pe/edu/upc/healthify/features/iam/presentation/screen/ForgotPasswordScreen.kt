package pe.edu.upc.healthify.features.iam.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogo
import pe.edu.upc.healthify.core.designsystem.component.HealthifyLogoVariant
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.presentation.components.AuthCardLayout
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordEvent
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordStep
import pe.edu.upc.healthify.features.iam.presentation.state.ForgotPasswordUiState
import pe.edu.upc.healthify.features.iam.presentation.viewmodel.ForgotPasswordViewModel

// The Figma has no recovery frame; it reuses the sign-in frame (white logo on green).
private val LogoSize = DpSize(56.dp, 50.dp)
private val GreenShapeOffsetY = 75.dp

/** Password recovery actions that the stateful screen wires to the ViewModel. */
data class ForgotPasswordActions(
    val onEmailChange: (String) -> Unit = {},
    val onCodeChange: (String) -> Unit = {},
    val onNewPasswordChange: (String) -> Unit = {},
    val onSendCode: () -> Unit = {},
    val onResendCode: () -> Unit = {},
    val onVerifyCode: () -> Unit = {},
    val onSubmitNewPassword: () -> Unit = {},
    val onChangeEmail: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onGoToSignIn: () -> Unit = {},
    val onErrorDialogRetry: () -> Unit = {},
    val onErrorDialogDismiss: () -> Unit = {},
)

/** Password recovery (email → code → new password → done). */
@Composable
fun ForgotPasswordScreen(
    onGoToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ForgotPasswordEvent.NavigateToSignIn -> onGoToSignIn()
        }
    }
    BackHandler(onBack = viewModel::onBack)
    ForgotPasswordContent(
        state = state,
        actions = ForgotPasswordActions(
            onEmailChange = viewModel::onEmailChange,
            onCodeChange = viewModel::onCodeChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onSendCode = viewModel::onSendCode,
            onResendCode = viewModel::onResendCode,
            onVerifyCode = viewModel::onVerifyCode,
            onSubmitNewPassword = viewModel::onSubmitNewPassword,
            onChangeEmail = viewModel::onChangeEmail,
            onBack = viewModel::onBack,
            onGoToSignIn = viewModel::onGoToSignIn,
            onErrorDialogRetry = viewModel::onErrorDialogRetry,
            onErrorDialogDismiss = viewModel::onErrorDialogDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    actions: ForgotPasswordActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus()
        action()
    }
    val primaryAction: @Composable () -> Unit = {
        when (state.step) {
            ForgotPasswordStep.Email -> HealthifyButton(
                text = stringResource(
                    if (state.isSubmitting) R.string.forgot_password_sending else R.string.forgot_password_send,
                ),
                onClick = { submit(actions.onSendCode) },
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            ForgotPasswordStep.Code -> HealthifyButton(
                text = stringResource(
                    if (state.isSubmitting) R.string.forgot_password_verifying else R.string.forgot_password_verify,
                ),
                onClick = { submit(actions.onVerifyCode) },
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            ForgotPasswordStep.NewPassword -> HealthifyButton(
                text = stringResource(
                    if (state.isSubmitting) R.string.forgot_password_submitting else R.string.forgot_password_submit,
                ),
                onClick = { submit(actions.onSubmitNewPassword) },
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            ForgotPasswordStep.Done -> HealthifyButton(
                text = stringResource(R.string.forgot_password_go_to_sign_in),
                onClick = actions.onGoToSignIn,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    AuthCardLayout(
        background = MaterialTheme.colorScheme.secondary,
        greenShapeOffsetY = GreenShapeOffsetY,
        keyboardAction = primaryAction,
        modifier = modifier,
        header = { HealthifyLogo(variant = HealthifyLogoVariant.White, size = LogoSize) },
    ) { keyboardOpen ->
        if (state.isOffline && state.step != ForgotPasswordStep.Done) {
            OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        }
        when (state.step) {
            ForgotPasswordStep.Email -> EmailStep(state, actions, keyboardOpen, primaryAction)
            ForgotPasswordStep.Code -> CodeStep(state, actions, keyboardOpen, primaryAction)
            ForgotPasswordStep.NewPassword -> NewPasswordStep(state, actions, keyboardOpen, primaryAction)
            ForgotPasswordStep.Done -> DoneStep(primaryAction)
        }
    }

    if (state.showErrorDialog) {
        ServerErrorDialog(onRetry = actions.onErrorDialogRetry, onDismiss = actions.onErrorDialogDismiss)
    }
}

@Composable
private fun ColumnScope.EmailStep(
    state: ForgotPasswordUiState,
    actions: ForgotPasswordActions,
    keyboardOpen: Boolean,
    primaryAction: @Composable () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val focusManager = LocalFocusManager.current
    StepHeader(
        title = stringResource(R.string.forgot_password_title),
        body = stringResource(R.string.forgot_password_email_subtitle),
    )
    HealthifyTextField(
        value = state.email,
        onValueChange = actions.onEmailChange,
        label = stringResource(R.string.auth_email),
        placeholder = stringResource(R.string.auth_email_placeholder),
        errorText = if (state.invalidEmail) stringResource(R.string.forgot_password_email_error) else null,
        type = HealthifyTextFieldType.Email,
        imeAction = ImeAction.Send,
        keyboardActions = KeyboardActions(onSend = {
            focusManager.clearFocus()
            actions.onSendCode()
        }),
        enabled = !state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        if (!keyboardOpen) primaryAction()
        HealthifyButton(
            text = stringResource(R.string.forgot_password_back_to_sign_in),
            onClick = actions.onBack,
            style = HealthifyButtonStyle.Text,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.CodeStep(
    state: ForgotPasswordUiState,
    actions: ForgotPasswordActions,
    keyboardOpen: Boolean,
    primaryAction: @Composable () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    StepHeader(
        title = stringResource(R.string.forgot_password_title),
        body = stringResource(R.string.forgot_password_code_subtitle, state.email.trim()),
    )
    if (state.tokenExpired) Notice(text = stringResource(R.string.forgot_password_token_expired))
    if (state.codeResent) {
        Text(
            text = stringResource(R.string.forgot_password_resent),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    HealthifyTextField(
        value = state.code,
        onValueChange = actions.onCodeChange,
        label = stringResource(R.string.forgot_password_code_label),
        placeholder = stringResource(R.string.forgot_password_code_placeholder),
        errorText = if (state.invalidCode) stringResource(R.string.forgot_password_code_error) else null,
        type = HealthifyTextFieldType.Number,
        imeAction = ImeAction.Go,
        keyboardActions = KeyboardActions(onGo = {
            focusManager.clearFocus()
            actions.onVerifyCode()
        }),
        enabled = !state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
        if (!keyboardOpen) primaryAction()
        HealthifyButton(
            text = if (state.resendInSeconds > 0) {
                stringResource(R.string.forgot_password_resend_in, state.resendInSeconds)
            } else {
                stringResource(R.string.forgot_password_resend)
            },
            onClick = actions.onResendCode,
            style = HealthifyButtonStyle.Text,
            enabled = !state.isSubmitting && state.resendInSeconds == 0,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.forgot_password_change_email),
            onClick = actions.onChangeEmail,
            style = HealthifyButtonStyle.Text,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.NewPasswordStep(
    state: ForgotPasswordUiState,
    actions: ForgotPasswordActions,
    keyboardOpen: Boolean,
    primaryAction: @Composable () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    StepHeader(
        title = stringResource(R.string.forgot_password_title),
        body = stringResource(R.string.forgot_password_new_password_subtitle),
    )
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
        HealthifyTextField(
            value = state.newPassword,
            onValueChange = actions.onNewPasswordChange,
            label = stringResource(R.string.forgot_password_new_password),
            placeholder = stringResource(R.string.sign_up_password_placeholder),
            errorText = when {
                state.passwordReused -> stringResource(R.string.forgot_password_error_reused)
                state.weakPassword -> stringResource(R.string.sign_up_error_password)
                else -> null
            },
            type = HealthifyTextFieldType.Password,
            imeAction = ImeAction.Go,
            keyboardActions = KeyboardActions(onGo = {
                focusManager.clearFocus()
                actions.onSubmitNewPassword()
            }),
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.sign_up_password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
    if (!keyboardOpen) primaryAction()
}

@Composable
private fun ColumnScope.DoneStep(primaryAction: @Composable () -> Unit) {
    StepHeader(
        title = stringResource(R.string.forgot_password_done_title),
        body = stringResource(R.string.forgot_password_done_body),
    )
    primaryAction()
}

@Composable
private fun StepHeader(title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = scheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = body,
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
    )
}

/** Notice in the style of the sign-in "account locked" notice: the time to change the password ran out. */
@Composable
private fun Notice(text: String, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onErrorContainer,
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.errorContainer, MaterialTheme.shapes.large)
            .padding(dimens.space16)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Preview(name = "Forgot password - email", widthDp = 360, heightDp = 800)
@Composable
private fun ForgotPasswordEmailPreview() {
    HealthifyTheme { ForgotPasswordContent(state = ForgotPasswordUiState(), actions = ForgotPasswordActions()) }
}

@Preview(name = "Forgot password - code", widthDp = 360, heightDp = 800)
@Composable
private fun ForgotPasswordCodePreview() {
    HealthifyTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(
                step = ForgotPasswordStep.Code,
                email = "maria.flores@correo.com",
                code = "12345",
                invalidCode = true,
                resendInSeconds = 42,
            ),
            actions = ForgotPasswordActions(),
        )
    }
}

@Preview(name = "Forgot password - time expired", widthDp = 360, heightDp = 800)
@Composable
private fun ForgotPasswordTokenExpiredPreview() {
    HealthifyTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(
                step = ForgotPasswordStep.Code,
                email = "maria.flores@correo.com",
                tokenExpired = true,
            ),
            actions = ForgotPasswordActions(),
        )
    }
}

@Preview(name = "Forgot password - new password", widthDp = 360, heightDp = 800)
@Composable
private fun ForgotPasswordNewPasswordPreview() {
    HealthifyTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(
                step = ForgotPasswordStep.NewPassword,
                newPassword = "Secreta#2026",
                passwordReused = true,
            ),
            actions = ForgotPasswordActions(),
        )
    }
}

@Preview(name = "Forgot password - done", widthDp = 360, heightDp = 800)
@Composable
private fun ForgotPasswordDonePreview() {
    HealthifyTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(step = ForgotPasswordStep.Done),
            actions = ForgotPasswordActions(),
        )
    }
}
