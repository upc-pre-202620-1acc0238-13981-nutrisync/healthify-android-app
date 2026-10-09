package pe.edu.upc.healthify.features.iam.presentation.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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
import pe.edu.upc.healthify.core.designsystem.component.SegmentedOption
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole
import pe.edu.upc.healthify.features.iam.presentation.components.AuthCardLayout
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEmailError
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpEvent
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpFieldErrors
import pe.edu.upc.healthify.features.iam.presentation.state.SignUpUiState
import pe.edu.upc.healthify.features.iam.presentation.viewmodel.SignUpViewModel

// Medidas del frame «S3 · Registro».
private val LogoSize = DpSize(44.dp, 39.dp)
private val GreenShapeOffsetY = 9.dp

/** Acciones de S3 que la pantalla con estado conecta con el ViewModel. */
data class SignUpActions(
    val onGivenNamesChange: (String) -> Unit = {},
    val onFamilyNamesChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onRoleSelect: (UserRole) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onErrorDialogRetry: () -> Unit = {},
    val onErrorDialogDismiss: () -> Unit = {},
)

/** S3 · Registro (Sin elegir / Paciente / Nutricionista · Carga · Sin conexión · S3.E · S3.K). */
@Composable
fun SignUpScreen(
    onSignedIn: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SignUpEvent.NavigateToShellLoading -> onSignedIn()
            SignUpEvent.NavigateToSignIn -> onSignIn()
        }
    }
    SignUpContent(
        state = state,
        actions = SignUpActions(
            onGivenNamesChange = viewModel::onGivenNamesChange,
            onFamilyNamesChange = viewModel::onFamilyNamesChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onRoleSelect = viewModel::onRoleSelect,
            onSubmit = viewModel::onSubmit,
            onSignIn = viewModel::onSignInClick,
            onErrorDialogRetry = viewModel::onErrorDialogRetry,
            onErrorDialogDismiss = viewModel::onErrorDialogDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun SignUpContent(
    state: SignUpUiState,
    actions: SignUpActions,
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
            text = stringResource(if (state.isSubmitting) R.string.sign_up_submitting else R.string.sign_up_submit),
            onClick = submit,
            loading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    AuthCardLayout(
        background = scheme.primary,
        greenShapeOffsetY = GreenShapeOffsetY,
        keyboardAction = submitButton,
        modifier = modifier,
    ) { keyboardOpen ->
        if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sign_up_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                HealthifyLogo(variant = HealthifyLogoVariant.Color, size = LogoSize)
            }
            Text(
                text = stringResource(R.string.sign_up_subtitle),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.primary,
            )
            NameFields(state = state, actions = actions)
        }
        HealthifyTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_placeholder),
            errorText = state.errors.email?.let { stringResource(it.messageRes()) },
            type = HealthifyTextFieldType.Email,
            imeAction = ImeAction.Next,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            HealthifyTextField(
                value = state.password,
                onValueChange = actions.onPasswordChange,
                label = stringResource(R.string.auth_password),
                placeholder = stringResource(R.string.sign_up_password_placeholder),
                errorText = if (state.errors.weakPassword) stringResource(R.string.sign_up_error_password) else null,
                type = HealthifyTextFieldType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.sign_up_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        RoleSelector(state = state, onRoleSelect = actions.onRoleSelect)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            if (!keyboardOpen) submitButton()
            if (state.isOffline) {
                Text(
                    text = stringResource(R.string.sign_up_offline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!state.isSubmitting) {
                // El frame «Carga» no muestra «Ya tengo cuenta» mientras se crea la cuenta.
                HealthifyButton(
                    text = stringResource(R.string.sign_up_have_account),
                    onClick = actions.onSignIn,
                    style = HealthifyButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (state.showErrorDialog) {
        ServerErrorDialog(onRetry = actions.onErrorDialogRetry, onDismiss = actions.onErrorDialogDismiss)
    }
}

@Composable
private fun NameFields(state: SignUpUiState, actions: SignUpActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(HealthifyTheme.dimens.space12)) {
        HealthifyTextField(
            value = state.givenNames,
            onValueChange = actions.onGivenNamesChange,
            label = stringResource(R.string.sign_up_given_names),
            placeholder = stringResource(R.string.sign_up_given_names_placeholder),
            errorText = state.errors.givenNames?.let {
                stringResource(it.messageRes(R.string.sign_up_error_given_names_required))
            },
            type = HealthifyTextFieldType.Name,
            imeAction = ImeAction.Next,
            enabled = !state.isSubmitting,
            modifier = Modifier.weight(1f),
        )
        HealthifyTextField(
            value = state.familyNames,
            onValueChange = actions.onFamilyNamesChange,
            label = stringResource(R.string.sign_up_family_names),
            placeholder = stringResource(R.string.sign_up_family_names_placeholder),
            errorText = state.errors.familyNames?.let {
                stringResource(it.messageRes(R.string.sign_up_error_family_names_required))
            },
            type = HealthifyTextFieldType.Name,
            imeAction = ImeAction.Next,
            enabled = !state.isSubmitting,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RoleSelector(state: SignUpUiState, onRoleSelect: (UserRole) -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        Text(
            text = stringResource(R.string.sign_up_role_label),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(dimens.space12),
            ) {
                RoleOption(UserRole.PATIENT, R.string.sign_up_role_patient, state, onRoleSelect)
                RoleOption(UserRole.PRACTITIONER, R.string.sign_up_role_practitioner, state, onRoleSelect)
            }
            val hint = when {
                state.errors.roleMissing -> R.string.sign_up_error_role
                state.role == null -> R.string.sign_up_role_hint_unselected
                else -> R.string.sign_up_role_hint_selected
            }
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = if (state.errors.roleMissing) scheme.error else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RowScope.RoleOption(
    role: UserRole,
    @StringRes labelRes: Int,
    state: SignUpUiState,
    onRoleSelect: (UserRole) -> Unit,
) {
    SegmentedOption(
        text = stringResource(labelRes),
        selected = state.role == role,
        onClick = { onRoleSelect(role) },
        enabled = !state.isSubmitting,
        isError = state.errors.roleMissing,
        modifier = Modifier.weight(1f),
    )
}

@StringRes
private fun SignUpEmailError.messageRes(): Int = when (this) {
    SignUpEmailError.Invalid -> R.string.sign_up_error_email
    SignUpEmailError.AlreadyTaken -> R.string.sign_up_error_email_taken
}

@StringRes
private fun PersonName.Problem.messageRes(@StringRes requiredRes: Int): Int = when (this) {
    PersonName.Problem.REQUIRED -> requiredRes
    PersonName.Problem.INVALID_CHARACTERS -> R.string.sign_up_error_name_characters
    PersonName.Problem.TOO_LONG -> R.string.sign_up_error_name_too_long
}

@Preview(name = "S3 · Registro — Sin elegir", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpUnselectedPreview() {
    HealthifyTheme { SignUpContent(state = SignUpUiState(), actions = SignUpActions()) }
}

@Preview(name = "S3 · Registro — Paciente", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpPatientPreview() {
    HealthifyTheme { SignUpContent(state = SignUpUiState(role = UserRole.PATIENT), actions = SignUpActions()) }
}

@Preview(name = "S3 · Registro Nu — Carga", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpLoadingPreview() {
    HealthifyTheme {
        SignUpContent(
            state = SignUpUiState(role = UserRole.PRACTITIONER, isSubmitting = true),
            actions = SignUpActions(),
        )
    }
}

@Preview(name = "S3 · Registro Pa — Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpOfflinePreview() {
    HealthifyTheme {
        SignUpContent(state = SignUpUiState(role = UserRole.PATIENT, isOffline = true), actions = SignUpActions())
    }
}

@Preview(name = "S3.E · Registro — errores de validación", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpErrorsPreview() {
    HealthifyTheme {
        SignUpContent(
            state = SignUpUiState(
                email = "maria.flores@",
                password = "maria123",
                errors = SignUpFieldErrors(email = SignUpEmailError.Invalid, weakPassword = true),
            ),
            actions = SignUpActions(),
        )
    }
}

@Preview(name = "S3 · Registro — error inesperado", widthDp = 360, heightDp = 800)
@Composable
private fun SignUpServerErrorPreview() {
    HealthifyTheme {
        SignUpContent(state = SignUpUiState(role = UserRole.PATIENT, showErrorDialog = true), actions = SignUpActions())
    }
}
