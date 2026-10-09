package pe.edu.upc.healthify.features.main.presentation.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.main.presentation.viewmodel.SignOutEvent
import pe.edu.upc.healthify.features.main.presentation.viewmodel.SignOutUiState
import pe.edu.upc.healthify.features.main.presentation.viewmodel.SignOutViewModel

// Medidas del frame PT21.1.
private val LogoSize = DpSize(64.dp, 57.dp)
private val BadgeSize = 80.dp
private val BadgeIconSize = 36.dp

/**
 * PT21.1 / PR20.1 · Cerrar sesión: confirmación a pantalla completa con el estilo de S6 ([bodyRes]: el texto de cada
 * rol; el nutricionista lee que sus pacientes y sus datos quedan guardados). Funciona igual sin conexión; los
 * registros pendientes se conservan y se envían al volver a entrar (F3).
 */
@Composable
fun SignOutConfirmationScreen(
    onCancel: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes bodyRes: Int = R.string.sign_out_dialog_body,
    viewModel: SignOutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            SignOutEvent.SignedOut -> onSignedOut()
        }
    }
    SignOutConfirmationContent(
        state = state,
        onConfirm = viewModel::onSignOut,
        onCancel = onCancel,
        modifier = modifier,
        bodyRes = bodyRes,
    )
}

@Composable
fun SignOutConfirmationContent(
    state: SignOutUiState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes bodyRes: Int = R.string.sign_out_dialog_body,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.space16, vertical = dimens.space40),
        verticalArrangement = Arrangement.spacedBy(dimens.space24, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HealthifyLogo(variant = HealthifyLogoVariant.White, size = LogoSize, decorative = true)
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .clip(CircleShape)
                .background(scheme.onSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = HealthifyIcons.Logout,
                contentDescription = null,
                tint = scheme.secondary,
                modifier = Modifier.size(BadgeIconSize),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.sign_out_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.inverseOnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.inverseOnSurface,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HealthifyButton(
                text = stringResource(if (state.isSigningOut) R.string.sign_out_in_progress else R.string.sign_out_dialog_confirm),
                onClick = onConfirm,
                loading = state.isSigningOut,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.ds_cancel),
                onClick = onCancel,
                style = HealthifyButtonStyle.Text,
                enabled = !state.isSigningOut,
                contentColor = scheme.inversePrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(name = "PT21.1 · Cerrar sesión", widthDp = 360, heightDp = 800)
@Composable
private fun SignOutConfirmationPreview() {
    HealthifyTheme { SignOutConfirmationContent(state = SignOutUiState(), onConfirm = {}, onCancel = {}) }
}

@Preview(name = "PT21.1 · Cerrando sesión", widthDp = 360, heightDp = 800)
@Composable
private fun SignOutConfirmationLoadingPreview() {
    HealthifyTheme { SignOutConfirmationContent(state = SignOutUiState(isSigningOut = true), onConfirm = {}, onCancel = {}) }
}

@Preview(name = "PR20.1 · Cerrar sesión (nutricionista)", widthDp = 360, heightDp = 800)
@Composable
private fun PractitionerSignOutConfirmationPreview() {
    HealthifyTheme {
        SignOutConfirmationContent(
            state = SignOutUiState(),
            onConfirm = {},
            onCancel = {},
            bodyRes = R.string.practitioner_sign_out_body,
        )
    }
}
