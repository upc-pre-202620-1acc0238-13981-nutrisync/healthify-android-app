package pe.edu.upc.healthify.features.carerelationship.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.carerelationship.presentation.state.DischargeDialog
import pe.edu.upc.healthify.features.carerelationship.presentation.state.DischargePatientUiState
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.DischargePatientEvent
import pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel.DischargePatientViewModel

private val WarningIconSize = 48.dp

data class DischargePatientActions(
    val onBack: () -> Unit = {},
    val onReasonChange: (String) -> Unit = {},
    val onDischargeRequested: () -> Unit = {},
    val onConfirmed: () -> Unit = {},
    val onAlreadyDischargedConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)

/** PR18 · Alta clínica (pantalla oscura del frame) + PR18.M confirmación. */
@Composable
fun DischargePatientScreen(
    onBack: () -> Unit,
    onDischarged: (patientName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DischargePatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is DischargePatientEvent.Discharged -> onDischarged(event.patientName)
        }
    }
    DischargePatientContent(
        state = state,
        actions = DischargePatientActions(
            onBack = onBack,
            onReasonChange = viewModel::onReasonChange,
            onDischargeRequested = viewModel::onDischargeRequested,
            onConfirmed = viewModel::onConfirmed,
            onAlreadyDischargedConfirmed = viewModel::onAlreadyDischargedConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun DischargePatientContent(state: DischargePatientUiState, actions: DischargePatientActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(dimens.radiusXl)
    SystemBarsAppearance(darkBackground = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.inverseSurface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(
            title = stringResource(R.string.discharge_title),
            onBack = actions.onBack,
            containerColor = scheme.inverseSurface,
            contentColor = scheme.inverseOnSurface,
        )
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
                    .clip(cardShape)
                    .background(scheme.onSurface)
                    .padding(dimens.space16),
                verticalArrangement = Arrangement.spacedBy(dimens.space16),
            ) {
                Icon(
                    imageVector = HealthifyIcons.Warning,
                    contentDescription = null,
                    tint = scheme.inverseOnSurface,
                    modifier = Modifier
                        .size(WarningIconSize)
                        .align(Alignment.CenterHorizontally),
                )
                Text(
                    text = stringResource(R.string.discharge_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.inverseOnSurface,
                )
                // Texto clínico del profesional: se guarda tal cual (nunca se traduce).
                HealthifyTextField(
                    value = state.reason,
                    onValueChange = actions.onReasonChange,
                    label = stringResource(R.string.discharge_reason),
                    placeholder = stringResource(R.string.discharge_reason_placeholder),
                    errorText = if (state.reasonMissing) stringResource(R.string.discharge_reason_missing) else null,
                    enabled = !state.isDischarging,
                    singleLine = false,
                    labelColor = scheme.inverseOnSurface,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(dimens.space12)) {
                HealthifyButton(
                    text = stringResource(if (state.isDischarging) R.string.discharge_processing else R.string.discharge_action),
                    onClick = actions.onDischargeRequested,
                    loading = state.isDischarging,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth(),
                )
                HealthifyButton(
                    text = stringResource(R.string.ds_cancel),
                    onClick = actions.onBack,
                    style = HealthifyButtonStyle.Text,
                    contentColor = scheme.inversePrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    when (state.dialog) {
        DischargeDialog.CONFIRM -> HealthifyDialog(
            title = stringResource(R.string.discharge_dialog_title),
            text = stringResource(R.string.discharge_dialog_body),
            confirmLabel = stringResource(if (state.isDischarging) R.string.discharge_processing else R.string.discharge_dialog_confirm),
            onConfirm = actions.onConfirmed,
            onDismiss = actions.onDialogDismissed,
            destructive = true,
            confirmLoading = state.isDischarging,
        )
        DischargeDialog.ALREADY_DISCHARGED -> HealthifyDialog(
            title = stringResource(R.string.discharge_already_title),
            text = stringResource(R.string.discharge_already_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onAlreadyDischargedConfirmed,
            onDismiss = actions.onAlreadyDischargedConfirmed,
            dismissLabel = null,
        )
        DischargeDialog.NO_ACTIVE_LINK -> HealthifyDialog(
            title = stringResource(R.string.practitioner_no_active_link_title),
            text = stringResource(R.string.practitioner_no_active_link_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onDialogDismissed,
            onDismiss = actions.onDialogDismissed,
            dismissLabel = null,
        )
        DischargeDialog.SERVER_ERROR -> ServerErrorDialog(onRetry = actions.onConfirmed, onDismiss = actions.onDialogDismissed)
        null -> Unit
    }
}

@Preview(name = "PR18 · Alta clínica", widthDp = 360, heightDp = 800)
@Composable
private fun DischargePatientPreview() {
    HealthifyTheme { DischargePatientContent(state = DischargePatientUiState(), actions = DischargePatientActions()) }
}

@Preview(name = "PR18 · Falta motivo", widthDp = 360, heightDp = 800)
@Composable
private fun DischargePatientMissingReasonPreview() {
    HealthifyTheme {
        DischargePatientContent(state = DischargePatientUiState(reasonMissing = true), actions = DischargePatientActions())
    }
}

@Preview(name = "PR18.M · Confirmación", widthDp = 360, heightDp = 800)
@Composable
private fun DischargePatientConfirmPreview() {
    HealthifyTheme {
        DischargePatientContent(
            state = DischargePatientUiState(reason = "Objetivos alcanzados", dialog = DischargeDialog.CONFIRM),
            actions = DischargePatientActions(),
        )
    }
}

@Preview(name = "PR18 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun DischargePatientOfflinePreview() {
    HealthifyTheme {
        DischargePatientContent(state = DischargePatientUiState(isOffline = true), actions = DischargePatientActions())
    }
}
