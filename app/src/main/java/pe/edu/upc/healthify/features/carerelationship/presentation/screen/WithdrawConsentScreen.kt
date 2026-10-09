package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.core.designsystem.theme.elevation
import pe.edu.upc.healthify.features.carerelationship.presentation.state.WithdrawConsentEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.state.WithdrawConsentUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.WithdrawConsentViewModel

// Medidas del frame «PT23 · Retirar consentimiento»: ícono de advertencia de 54 × 51 dp.
private val WarningIconSize = 52.dp

/** Acciones de PT23 que la pantalla con estado conecta con el ViewModel. */
data class WithdrawConsentActions(
    val onBack: () -> Unit = {},
    val onWithdraw: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onConfirmDismiss: () -> Unit = {},
    val onNoActiveConsentAcknowledged: () -> Unit = {},
    val onServerErrorRetry: () -> Unit = {},
    val onServerErrorDismiss: () -> Unit = {},
)

/** PT23 · Retirar consentimiento (+ PT23.M confirmación final). */
@Composable
fun WithdrawConsentScreen(
    onBack: () -> Unit,
    onWithdrawn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WithdrawConsentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            WithdrawConsentEvent.NavigateToWithdrawn -> onWithdrawn()
        }
    }
    WithdrawConsentContent(
        state = state,
        actions = WithdrawConsentActions(
            onBack = onBack,
            onWithdraw = viewModel::onWithdrawClick,
            onConfirm = viewModel::onConfirmWithdraw,
            onConfirmDismiss = viewModel::onConfirmDismiss,
            onNoActiveConsentAcknowledged = viewModel::onNoActiveConsentAcknowledged,
            onServerErrorRetry = viewModel::onServerErrorRetry,
            onServerErrorDismiss = viewModel::onServerErrorDismiss,
        ),
        modifier = modifier,
    )
}

@Composable
fun WithdrawConsentContent(
    state: WithdrawConsentUiState,
    actions: WithdrawConsentActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(dimens.radiusXl)
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.withdraw_consent_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = dimens.space16, top = dimens.space16, end = dimens.space16, bottom = dimens.space40),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevation(dimens.elevation1, cardShape)
                    .clip(cardShape)
                    .background(scheme.surfaceContainerLowest)
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
            ) {
                Icon(
                    imageVector = HealthifyIcons.Warning,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier
                        .size(WarningIconSize)
                        .align(Alignment.CenterHorizontally),
                )
                Text(
                    text = stringResource(R.string.withdraw_consent_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.withdraw_consent_no_reason),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                HealthifyButton(
                    text = stringResource(R.string.withdraw_consent_action),
                    onClick = actions.onWithdraw,
                    // Nota PT23 «Sin conexión: botón deshabilitado».
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth(),
                )
                HealthifyButton(
                    text = stringResource(R.string.ds_cancel),
                    onClick = actions.onBack,
                    style = HealthifyButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    when {
        // PT23.M · confirmación final; «Retirando…» en el botón mientras se envía.
        state.showConfirmDialog -> HealthifyDialog(
            title = stringResource(R.string.withdraw_consent_dialog_title),
            text = stringResource(R.string.withdraw_consent_dialog_body),
            confirmLabel = stringResource(
                if (state.isWithdrawing) R.string.withdraw_consent_withdrawing else R.string.withdraw_consent_dialog_confirm,
            ),
            onConfirm = actions.onConfirm,
            onDismiss = actions.onConfirmDismiss,
            destructive = true,
            confirmLoading = state.isWithdrawing,
        )
        state.showNoActiveConsent -> HealthifyDialog(
            title = stringResource(R.string.withdraw_consent_inactive_title),
            text = stringResource(R.string.withdraw_consent_inactive_body),
            confirmLabel = stringResource(R.string.withdraw_consent_inactive_action),
            onConfirm = actions.onNoActiveConsentAcknowledged,
            onDismiss = actions.onNoActiveConsentAcknowledged,
            dismissLabel = null,
        )
        state.showServerError -> ServerErrorDialog(
            onRetry = actions.onServerErrorRetry,
            onDismiss = actions.onServerErrorDismiss,
        )
    }
}

@Preview(name = "PT23 · Retirar consentimiento", widthDp = 360, heightDp = 800)
@Composable
private fun WithdrawConsentPreview() {
    HealthifyTheme { WithdrawConsentContent(state = WithdrawConsentUiState(), actions = WithdrawConsentActions()) }
}

@Preview(name = "PT23.M · Retirar consentimiento — confirmación final", widthDp = 360, heightDp = 800)
@Composable
private fun WithdrawConsentConfirmPreview() {
    HealthifyTheme {
        WithdrawConsentContent(
            state = WithdrawConsentUiState(showConfirmDialog = true),
            actions = WithdrawConsentActions(),
        )
    }
}

@Preview(name = "PT23.M · Retirando… (carga)", widthDp = 360, heightDp = 800)
@Composable
private fun WithdrawConsentLoadingPreview() {
    HealthifyTheme {
        WithdrawConsentContent(
            state = WithdrawConsentUiState(showConfirmDialog = true, isWithdrawing = true),
            actions = WithdrawConsentActions(),
        )
    }
}

@Preview(name = "PT23 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun WithdrawConsentOfflinePreview() {
    HealthifyTheme {
        WithdrawConsentContent(state = WithdrawConsentUiState(isOffline = true), actions = WithdrawConsentActions())
    }
}

@Preview(name = "PT23 · Consentimiento ya no vigente", widthDp = 360, heightDp = 800)
@Composable
private fun WithdrawConsentInactivePreview() {
    HealthifyTheme {
        WithdrawConsentContent(
            state = WithdrawConsentUiState(showNoActiveConsent = true),
            actions = WithdrawConsentActions(),
        )
    }
}
