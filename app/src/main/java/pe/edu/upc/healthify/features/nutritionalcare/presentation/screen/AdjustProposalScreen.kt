package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditField
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ReviewDialogHost
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.AdjustProposalUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewLoadError
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.AdjustProposalEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.AdjustProposalViewModel

data class AdjustProposalActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onEnergyChange: (String) -> Unit = {},
    val onProteinChange: (String) -> Unit = {},
    val onCarbChange: (String) -> Unit = {},
    val onFatChange: (String) -> Unit = {},
    val onGuidelineToggle: (PlanGuidelineCode, Boolean) -> Unit = { _, _ -> },
    val onMessageChange: (String) -> Unit = {},
    val onAssign: () -> Unit = {},
    val onDialogConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)

/** PR14.IA-A · Ajustar el plan propuesto por IA (todo prellenado; «Asignar plan ajustado»). */
@Composable
fun AdjustProposalScreen(
    onBack: () -> Unit,
    onAssigned: (patientName: String?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdjustProposalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is AdjustProposalEvent.Assigned -> onAssigned(event.patientName)
            AdjustProposalEvent.Close -> onClose()
        }
    }
    AdjustProposalContent(
        state = state,
        actions = AdjustProposalActions(
            onBack = onBack,
            onRetryLoad = viewModel::onRetryLoad,
            onClose = onClose,
            onEnergyChange = viewModel::onEnergyChange,
            onProteinChange = viewModel::onProteinChange,
            onCarbChange = viewModel::onCarbChange,
            onFatChange = viewModel::onFatChange,
            onGuidelineToggle = viewModel::onGuidelineToggle,
            onMessageChange = viewModel::onMessageChange,
            onAssign = viewModel::onAssign,
            onDialogConfirmed = viewModel::onDialogConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun AdjustProposalContent(state: AdjustProposalUiState, actions: AdjustProposalActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.adjust_proposal_title), onBack = actions.onBack)
        val ready = !state.isLoading && state.loadError == null
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space16),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            when {
                state.isLoading -> repeat(2) { SkeletonCard(modifier = Modifier.fillMaxWidth()) }
                state.loadError == ReviewLoadError.NOT_AVAILABLE -> EmptyState(
                    title = stringResource(R.string.review_not_available_title),
                    text = stringResource(R.string.review_not_available_body),
                    actionLabel = stringResource(R.string.review_back_to_inbox),
                    onAction = actions.onClose,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadError == ReviewLoadError.OFFLINE -> EmptyState(
                    title = stringResource(R.string.practitioner_offline_title),
                    text = stringResource(R.string.inbox_offline_body),
                    actionLabel = stringResource(R.string.ds_retry),
                    onAction = actions.onRetryLoad,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.loadError != null -> ErrorState(onRetry = actions.onRetryLoad, modifier = Modifier.fillMaxWidth())
                else -> AdjustForm(state, actions)
            }
        }
        if (ready) {
            HealthifyButton(
                text = stringResource(if (state.isAssigning) R.string.review_assigning else R.string.adjust_proposal_assign),
                onClick = actions.onAssign,
                loading = state.isAssigning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.space16),
            )
        }
    }
    ReviewDialogHost(dialog = state.dialog, onConfirm = actions.onDialogConfirmed, onDismiss = actions.onDialogDismissed)
}

@Composable
private fun AdjustForm(state: AdjustProposalUiState, actions: AdjustProposalActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val enabled = !state.isAssigning
    val positiveError = stringResource(R.string.adjust_proposal_positive_value)
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        AiBadge(label = stringResource(R.string.adjust_proposal_badge))
        Text(
            text = stringResource(R.string.adjust_proposal_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
    }
    SectionCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = dimens.space16) {
        Text(text = stringResource(R.string.adjust_proposal_targets), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        val energyError = when {
            state.belowSafetyFloor -> stringResource(R.string.adjust_proposal_below_floor)
            ProposalEditField.ENERGY in state.invalidFields -> positiveError
            else -> null
        }
        HealthifyTextField(
            value = state.energy,
            onValueChange = actions.onEnergyChange,
            label = stringResource(R.string.adjust_proposal_energy),
            supportingText = state.previousEnergyKcal?.let { UiText.of(R.string.adjust_proposal_before, it).asString() },
            errorText = energyError,
            enabled = enabled,
            type = HealthifyTextFieldType.Decimal,
            trailingIcon = { UnitSuffix(stringResource(R.string.unit_kcal)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            MacroField(
                value = state.protein,
                onValueChange = actions.onProteinChange,
                label = stringResource(R.string.adjust_proposal_protein),
                isError = ProposalEditField.PROTEIN in state.invalidFields,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = state.carb,
                onValueChange = actions.onCarbChange,
                label = stringResource(R.string.adjust_proposal_carb),
                isError = ProposalEditField.CARB in state.invalidFields,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            MacroField(
                value = state.fat,
                onValueChange = actions.onFatChange,
                label = stringResource(R.string.adjust_proposal_fat),
                isError = ProposalEditField.FAT in state.invalidFields,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        val macroInvalid = state.invalidFields.any {
            it == ProposalEditField.PROTEIN || it == ProposalEditField.CARB || it == ProposalEditField.FAT
        }
        if (macroInvalid) ErrorLine(positiveError)
    }
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.adjust_proposal_guidelines), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            PlanGuidelineCode.entries.forEach { code ->
                HealthifyFilterChip(
                    text = stringResource(code.labelRes()),
                    selected = code in state.guidelines,
                    onSelectedChange = { actions.onGuidelineToggle(code, it) },
                    enabled = enabled,
                )
            }
        }
    }
    HealthifyTextField(
        value = state.message,
        onValueChange = actions.onMessageChange,
        label = state.patientFirstName?.let { stringResource(R.string.proposal_message_for, it) }
            ?: stringResource(R.string.proposal_message_for_patient),
        supportingText = stringResource(R.string.adjust_proposal_message_help, PatientMessage.MAX_LENGTH),
        errorText = if (ProposalEditField.MESSAGE in state.invalidFields) {
            stringResource(R.string.adjust_proposal_message_invalid, PatientMessage.MAX_LENGTH)
        } else {
            null
        },
        enabled = enabled,
        singleLine = false,
    )
}

@Composable
private fun MacroField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    HealthifyTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        enabled = enabled,
        type = HealthifyTextFieldType.Decimal,
        trailingIcon = { UnitSuffix(stringResource(R.string.unit_g)) },
        modifier = modifier,
    )
}

private val previewState = AdjustProposalUiState(
    isLoading = false,
    patientFirstName = "Ana",
    previousEnergyKcal = 1796.0,
    energy = "1650",
    protein = "119",
    carb = "170",
    fat = "55",
    guidelines = setOf(
        PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER,
        PlanGuidelineCode.PRIORITIZE_VEGETABLES,
        PlanGuidelineCode.AVOID_SUGARY_DRINKS,
        PlanGuidelineCode.REDUCE_SALT,
        PlanGuidelineCode.DRINK_2L_WATER,
    ),
    message = "Notamos que tus cenas son más ligeras. Probemos con estas ideas.",
)

@Preview(name = "PR14.IA-A · Ajustar plan", widthDp = 360, heightDp = 1100)
@Composable
private fun AdjustProposalPreview() {
    HealthifyTheme { AdjustProposalContent(state = previewState, actions = AdjustProposalActions()) }
}

@Preview(name = "PR14.IA-A · Bajo el piso calórico", widthDp = 360, heightDp = 800)
@Composable
private fun AdjustProposalFloorPreview() {
    HealthifyTheme {
        AdjustProposalContent(state = previewState.copy(energy = "900", belowSafetyFloor = true), actions = AdjustProposalActions())
    }
}

@Preview(name = "PR14.IA-A · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun AdjustProposalLoadingPreview() {
    HealthifyTheme { AdjustProposalContent(state = AdjustProposalUiState(), actions = AdjustProposalActions()) }
}

@Preview(name = "PR14.IA-A · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun AdjustProposalOfflinePreview() {
    HealthifyTheme {
        AdjustProposalContent(
            state = AdjustProposalUiState(isLoading = false, isOffline = true, loadError = ReviewLoadError.OFFLINE),
            actions = AdjustProposalActions(),
        )
    }
}
