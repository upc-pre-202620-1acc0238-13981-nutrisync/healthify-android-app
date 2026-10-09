package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.EmptyState
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.core.designsystem.component.YesNoSelector
import pe.edu.upc.healthify.core.designsystem.component.asString
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAdjustmentProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalStatus
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ProposalRow
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ReviewDialogHost
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ReviewEvidenceCard
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ProposalSection
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewItemHeader
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewItemUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ReviewLoadError
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ReviewItemViewModel
import java.time.Instant

private val GeneratingIndicatorSize = 20.dp
private val GeneratingIndicatorStroke = 2.dp

data class ReviewItemActions(
    val onBack: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onAdjustedChange: (Boolean) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onResolve: () -> Unit = {},
    val onAdjustPlanNow: () -> Unit = {},
    val onAcceptAsIs: () -> Unit = {},
    val onAdjust: () -> Unit = {},
    val onResolveWithoutProposal: () -> Unit = {},
    val onDialogConfirmed: () -> Unit = {},
    val onDialogDismissed: () -> Unit = {},
)

/** PR14 · Resolver ítem (+ PR14.1) y PR14.IA · Revisar señal con plan propuesto por IA. */
@Composable
fun ReviewItemScreen(
    onBack: () -> Unit,
    onResolved: (patientName: String?) -> Unit,
    onClose: () -> Unit,
    onOpenAdjust: (reviewItemId: Long) -> Unit,
    onOpenPatient: (patientId: Long, careLinkId: Long, patientName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewItemViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is ReviewItemEvent.Resolved -> onResolved(event.patientName)
            is ReviewItemEvent.OpenAdjust -> onOpenAdjust(event.reviewItemId)
            is ReviewItemEvent.OpenPatient -> onOpenPatient(event.patientId, event.careLinkId, event.patientName)
            ReviewItemEvent.Close -> onClose()
        }
    }
    ReviewItemContent(
        state = state,
        actions = ReviewItemActions(
            onBack = onBack,
            onRetryLoad = viewModel::onRetryLoad,
            onClose = onClose,
            onAdjustedChange = viewModel::onAdjustedChange,
            onNoteChange = viewModel::onNoteChange,
            onResolve = viewModel::onResolve,
            onAdjustPlanNow = viewModel::onAdjustPlanNow,
            onAcceptAsIs = viewModel::onAcceptAsIs,
            onAdjust = viewModel::onAdjust,
            onResolveWithoutProposal = viewModel::onResolveWithoutProposal,
            onDialogConfirmed = viewModel::onDialogConfirmed,
            onDialogDismissed = viewModel::onDialogDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun ReviewItemContent(state: ReviewItemUiState, actions: ReviewItemActions, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.review_title), onBack = actions.onBack)
        val header = state.header
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
                state.loadError != null -> LoadErrorBody(state.loadError, actions)
                header != null -> ReviewBody(state, header, actions)
            }
        }
        if (header != null && !state.isLoading && state.loadError == null) BottomActions(state, actions)
    }
    ReviewDialogHost(dialog = state.dialog, onConfirm = actions.onDialogConfirmed, onDismiss = actions.onDialogDismissed)
}

@Composable
private fun LoadErrorBody(error: ReviewLoadError, actions: ReviewItemActions) {
    when (error) {
        ReviewLoadError.NOT_AVAILABLE -> EmptyState(
            title = stringResource(R.string.review_not_available_title),
            text = stringResource(R.string.review_not_available_body),
            actionLabel = stringResource(R.string.review_back_to_inbox),
            onAction = actions.onClose,
            modifier = Modifier.fillMaxWidth(),
        )
        ReviewLoadError.OFFLINE -> EmptyState(
            title = stringResource(R.string.practitioner_offline_title),
            text = stringResource(R.string.inbox_offline_body),
            actionLabel = stringResource(R.string.ds_retry),
            onAction = actions.onRetryLoad,
            modifier = Modifier.fillMaxWidth(),
        )
        ReviewLoadError.GENERIC -> ErrorState(onRetry = actions.onRetryLoad, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ColumnScope.ReviewBody(state: ReviewItemUiState, header: ReviewItemHeader, actions: ReviewItemActions) {
    ReviewEvidenceCard(header = header)
    when (val proposal = state.proposal) {
        is ProposalSection.Ready -> {
            ProposalCard(proposal = proposal.proposal, patientName = header.patientName)
            if (!state.showManualResolution) {
                HealthifyButton(
                    text = stringResource(R.string.review_resolve_without_proposal),
                    onClick = actions.onResolveWithoutProposal,
                    style = HealthifyButtonStyle.Text,
                    enabled = !state.isBusy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        ProposalSection.Generating -> GeneratingCard(
            showResolveNow = !state.showManualResolution,
            onResolveNow = actions.onResolveWithoutProposal,
        )
        ProposalSection.NotOffered -> Unit
    }
    if (state.showsResolutionForm) ResolutionForm(state, actions)
}

@Composable
private fun ResolutionForm(state: ReviewItemUiState, actions: ReviewItemActions) {
    val dimens = HealthifyTheme.dimens
    SectionCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = dimens.space16) {
        YesNoSelector(
            question = stringResource(R.string.review_adjusted_question),
            answer = state.adjusted,
            onAnswer = actions.onAdjustedChange,
            enabled = !state.isBusy,
            isError = state.showOutcomeError,
            errorText = if (state.showOutcomeError) stringResource(R.string.review_answer_yes_no) else null,
        )
        HealthifyTextField(
            value = state.note,
            onValueChange = actions.onNoteChange,
            label = stringResource(R.string.review_note_label),
            placeholder = stringResource(R.string.review_note_placeholder),
            enabled = !state.isBusy,
            singleLine = false,
        )
    }
    Text(
        text = stringResource(R.string.review_resolution_info),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** PR14.IA · card «Plan propuesto por IA» (contorno `tertiary`). Leerla nunca cambia el plan. */
@Composable
private fun ProposalCard(proposal: PlanAdjustmentProposal, patientName: String?) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(modifier = Modifier.fillMaxWidth(), borderColor = scheme.tertiary, verticalSpacing = dimens.space16) {
        AiBadge(label = stringResource(R.string.review_ai_proposal_badge))
        Text(text = proposal.title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
        val energy = proposal.currentEnergyKcal
            ?.let { UiText.of(R.string.proposal_energy_change, it, proposal.proposed.energyKcal) }
            ?: UiText.of(R.string.proposal_energy_value, proposal.proposed.energyKcal)
        ProposalRow(icon = HealthifyIcons.Edit, title = stringResource(R.string.proposal_daily_energy), supportingText = energy.asString())
        proposal.addedGuidelines.forEach { code ->
            ProposalRow(
                icon = HealthifyIcons.Add,
                title = stringResource(R.string.proposal_added_guideline),
                supportingText = stringResource(code.labelRes()),
            )
        }
        proposal.removedGuidelines.forEach { code ->
            ProposalRow(
                icon = HealthifyIcons.Close,
                title = stringResource(R.string.proposal_removed_guideline),
                supportingText = stringResource(code.labelRes()),
            )
        }
        if (proposal.patientMessage.isNotBlank()) {
            val firstName = patientName?.substringBefore(' ')
            ProposalRow(
                icon = HealthifyIcons.Info,
                title = firstName?.let { stringResource(R.string.proposal_message_for, it) }
                    ?: stringResource(R.string.proposal_message_for_patient),
                // Texto de la IA en el idioma del paciente: se muestra tal cual.
                supportingText = stringResource(R.string.proposal_quoted, proposal.patientMessage),
            )
        }
        ProposalRow(
            icon = HealthifyIcons.Calendar,
            title = stringResource(R.string.proposal_follow_up),
            supportingText = stringResource(R.string.proposal_recheck_in_days, proposal.recheckAfterDays),
        )
        if (proposal.rationale.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(scheme.surfaceContainer)
                    .padding(dimens.space12),
                verticalArrangement = Arrangement.spacedBy(dimens.space4),
            ) {
                Text(text = stringResource(R.string.proposal_why), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                Text(text = proposal.rationale, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
        }
    }
}

/** `202`: la IA está preparando la propuesta; se puede resolver sin esperarla. */
@Composable
private fun GeneratingCard(showResolveNow: Boolean, onResolveNow: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(modifier = Modifier.fillMaxWidth(), borderColor = scheme.tertiary) {
        AiBadge(label = stringResource(R.string.review_ai_proposal_badge))
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space12), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(GeneratingIndicatorSize),
                color = scheme.primary,
                strokeWidth = GeneratingIndicatorStroke,
            )
            Text(text = stringResource(R.string.review_ai_generating), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        }
        if (showResolveNow) {
            HealthifyButton(
                text = stringResource(R.string.review_resolve_now),
                onClick = onResolveNow,
                style = HealthifyButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun BottomActions(state: ReviewItemUiState, actions: ReviewItemActions) {
    val dimens = HealthifyTheme.dimens
    val proposalReady = state.proposal is ProposalSection.Ready
    if (proposalReady && !state.showManualResolution) {
        // PR14.IA: «Ajustar» (Outlined) y «Resolver» (asigna tal cual) lado a lado.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            horizontalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = stringResource(R.string.review_adjust),
                onClick = actions.onAdjust,
                style = HealthifyButtonStyle.Outlined,
                enabled = !state.isBusy,
                modifier = Modifier.weight(1f),
            )
            HealthifyButton(
                text = stringResource(if (state.isAccepting) R.string.review_assigning else R.string.review_resolve),
                onClick = actions.onAcceptAsIs,
                loading = state.isAccepting,
                enabled = !state.isBusy,
                modifier = Modifier.weight(1f),
            )
        }
    } else if (state.showsResolutionForm) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            HealthifyButton(
                text = stringResource(if (state.isResolving) R.string.common_saving else R.string.review_resolve),
                onClick = actions.onResolve,
                loading = state.isResolving,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.review_adjust_plan_now),
                onClick = actions.onAdjustPlanNow,
                style = HealthifyButtonStyle.Outlined,
                loading = state.isOpeningPatient,
                enabled = !state.isBusy && !state.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val previewHeader = ReviewItemHeader(
    patientName = "Ana Flores",
    signal = UiText.Raw("Desviación sostenida"),
    receivedAt = Instant.parse("2026-09-08T15:00:00Z"),
    evidence = UiText.Raw("Ana Flores registró en promedio 40 % menos de su meta de energía en 5 de sus últimos 9 días registrados."),
    mentionsLoggedDays = true,
)

private val previewProposal = PlanAdjustmentProposal(
    reviewItemId = ReviewItemId(1),
    title = "Ajustar la energía y reforzar las cenas",
    currentEnergyKcal = 1796.0,
    proposed = Targets(1650.0, 119.0, 170.0, 55.0),
    addedGuidelines = listOf(PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER),
    removedGuidelines = emptyList(),
    patientMessage = "Notamos que tus cenas son más ligeras. Probemos con estas ideas.",
    recheckAfterDays = 7,
    rationale = "Registra menos energía sobre todo en la cena (5 de 9 días) y su peso baja más rápido de lo indicado (−0.6 kg/sem).",
    generatedAt = Instant.parse("2026-09-08T16:00:00Z"),
    status = PlanProposalStatus.PROPOSED,
)

@Preview(name = "PR14 · Resolver ítem", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewItemPreview() {
    HealthifyTheme { ReviewItemContent(state = ReviewItemUiState(isLoading = false, header = previewHeader), actions = ReviewItemActions()) }
}

@Preview(name = "PR14.1 · Responde Sí o No", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewItemOutcomeErrorPreview() {
    HealthifyTheme {
        ReviewItemContent(
            state = ReviewItemUiState(isLoading = false, header = previewHeader, showOutcomeError = true),
            actions = ReviewItemActions(),
        )
    }
}

@Preview(name = "PR14.IA · Plan propuesto por IA", widthDp = 360, heightDp = 1000)
@Composable
private fun ReviewItemProposalPreview() {
    HealthifyTheme {
        ReviewItemContent(
            state = ReviewItemUiState(isLoading = false, header = previewHeader, proposal = ProposalSection.Ready(previewProposal)),
            actions = ReviewItemActions(),
        )
    }
}

@Preview(name = "PR14.IA · Generándose", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewItemGeneratingPreview() {
    HealthifyTheme {
        ReviewItemContent(
            state = ReviewItemUiState(isLoading = false, header = previewHeader, proposal = ProposalSection.Generating),
            actions = ReviewItemActions(),
        )
    }
}

@Preview(name = "PR14 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewItemLoadingPreview() {
    HealthifyTheme { ReviewItemContent(state = ReviewItemUiState(), actions = ReviewItemActions()) }
}

@Preview(name = "PR14 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun ReviewItemOfflinePreview() {
    HealthifyTheme {
        ReviewItemContent(
            state = ReviewItemUiState(isLoading = false, isOffline = true, loadError = ReviewLoadError.OFFLINE),
            actions = ReviewItemActions(),
        )
    }
}
