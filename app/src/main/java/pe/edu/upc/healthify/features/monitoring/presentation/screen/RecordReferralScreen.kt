package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralField
import pe.edu.upc.healthify.features.monitoring.presentation.state.RecordReferralUiState
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.RecordReferralEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.RecordReferralViewModel

data class RecordReferralActions(
    val onBack: () -> Unit = {},
    val onSpecialtyChange: (String) -> Unit = {},
    val onReasonChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBackToForm: () -> Unit = {},
    val onNoActiveLinkDismissed: () -> Unit = {},
)

/** PR16 · Registrar derivación (+ PR16.E campos obligatorios, PR16.2 error al registrar). */
@Composable
fun RecordReferralScreen(
    onBack: () -> Unit,
    onRecorded: (specialty: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordReferralViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is RecordReferralEvent.Recorded -> onRecorded(event.specialty)
        }
    }
    RecordReferralContent(
        state = state,
        actions = RecordReferralActions(
            onBack = onBack,
            onSpecialtyChange = viewModel::onSpecialtyChange,
            onReasonChange = viewModel::onReasonChange,
            onSubmit = viewModel::onSubmit,
            onBackToForm = viewModel::onBackToForm,
            onNoActiveLinkDismissed = viewModel::onNoActiveLinkDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun RecordReferralContent(state: RecordReferralUiState, actions: RecordReferralActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    if (state.failed) {
        FailureState(
            title = stringResource(R.string.referral_failed_title),
            text = stringResource(R.string.referral_failed_body),
            actionLabel = stringResource(R.string.common_try_again),
            onAction = actions.onBackToForm,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.referral_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            SectionCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = dimens.space16) {
                HealthifyTextField(
                    value = state.specialty,
                    onValueChange = actions.onSpecialtyChange,
                    label = stringResource(R.string.referral_specialty),
                    placeholder = stringResource(R.string.referral_specialty_placeholder),
                    errorText = if (ReferralField.SPECIALTY in state.missing) stringResource(R.string.referral_specialty_missing) else null,
                    enabled = !state.isSaving,
                    imeAction = ImeAction.Next,
                )
                // Texto clínico del profesional: se guarda y se muestra tal cual.
                HealthifyTextField(
                    value = state.reason,
                    onValueChange = actions.onReasonChange,
                    label = stringResource(R.string.referral_reason),
                    placeholder = stringResource(R.string.referral_reason_placeholder),
                    errorText = if (ReferralField.REASON in state.missing) stringResource(R.string.referral_reason_missing) else null,
                    enabled = !state.isSaving,
                    singleLine = false,
                )
            }
            HealthifyButton(
                text = stringResource(if (state.isSaving) R.string.common_saving else R.string.referral_submit),
                onClick = actions.onSubmit,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.noActiveLink) {
        HealthifyDialog(
            title = stringResource(R.string.practitioner_no_active_link_title),
            text = stringResource(R.string.practitioner_no_active_link_body),
            confirmLabel = stringResource(R.string.common_understood),
            onConfirm = actions.onNoActiveLinkDismissed,
            onDismiss = actions.onNoActiveLinkDismissed,
            dismissLabel = null,
        )
    }
}

@Preview(name = "PR16 · Registrar derivación", widthDp = 360, heightDp = 800)
@Composable
private fun RecordReferralPreview() {
    HealthifyTheme { RecordReferralContent(state = RecordReferralUiState(), actions = RecordReferralActions()) }
}

@Preview(name = "PR16.E · Campos obligatorios", widthDp = 360, heightDp = 800)
@Composable
private fun RecordReferralMissingPreview() {
    HealthifyTheme {
        RecordReferralContent(
            state = RecordReferralUiState(missing = setOf(ReferralField.SPECIALTY, ReferralField.REASON)),
            actions = RecordReferralActions(),
        )
    }
}

@Preview(name = "PR16.2 · Error al registrar", widthDp = 360, heightDp = 800)
@Composable
private fun RecordReferralFailedPreview() {
    HealthifyTheme { RecordReferralContent(state = RecordReferralUiState(failed = true), actions = RecordReferralActions()) }
}

@Preview(name = "PR16 · Guardando", widthDp = 360, heightDp = 800)
@Composable
private fun RecordReferralSavingPreview() {
    HealthifyTheme {
        RecordReferralContent(
            state = RecordReferralUiState(specialty = "Endocrinología", reason = "Hipotiroidismo sin control", isSaving = true),
            actions = RecordReferralActions(),
        )
    }
}
