package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.AiBadge
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTag
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.CustomGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ConsultationStepScaffold
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PublicationUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ConsultationStepEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.PublicationStepViewModel

/** Acciones de EV-5. */
data class PublicationActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRestrictionToggle: (PlanRestrictionCode) -> Unit = {},
    val onGuidelineToggle: (PlanGuidelineCode) -> Unit = {},
    val onAddCustomClick: () -> Unit = {},
    val onCustomDraftChange: (String) -> Unit = {},
    val onConfirmCustom: () -> Unit = {},
    val onRemoveCustom: (String) -> Unit = {},
    val onPatientMessageChange: (String) -> Unit = {},
    val onPublish: () -> Unit = {},
    val onRetryPublish: () -> Unit = {},
    val onPublishFailureDismissed: () -> Unit = {},
)

/**
 * EV-5 · Paso 4 Indicaciones y publicación (+ EV-5.E error al publicar). «Publicar y cerrar consulta» publica la nueva
 * versión (reemplaza la vigente, NC-7) y cierra la consulta.
 */
@Composable
fun PublicationStepScreen(
    onBack: (patientId: Long, patientName: String) -> Unit,
    onPublished: () -> Unit,
    onConsultationClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicationStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler {
        if (state.publishFailed) viewModel.onPublishFailureDismissed() else viewModel.onBack()
    }
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ConsultationStepEvent.Back -> onBack(viewModel.patientId, viewModel.patientName)
            ConsultationStepEvent.Published -> onPublished()
            ConsultationStepEvent.ConsultationClosed, ConsultationStepEvent.Exit -> onConsultationClosed()
            ConsultationStepEvent.Continue -> Unit
        }
    }
    PublicationStepContent(
        state = state,
        actions = PublicationActions(
            onBack = viewModel::onBack,
            onRetry = viewModel::onRetry,
            onRestrictionToggle = viewModel::onRestrictionToggle,
            onGuidelineToggle = viewModel::onGuidelineToggle,
            onAddCustomClick = viewModel::onAddCustomClick,
            onCustomDraftChange = viewModel::onCustomDraftChange,
            onConfirmCustom = viewModel::onConfirmCustom,
            onRemoveCustom = viewModel::onRemoveCustom,
            onPatientMessageChange = viewModel::onPatientMessageChange,
            onPublish = viewModel::onPublish,
            onRetryPublish = viewModel::onRetryPublish,
            onPublishFailureDismissed = viewModel::onPublishFailureDismissed,
        ),
        modifier = modifier,
    )
}

@Composable
fun PublicationStepContent(state: PublicationUiState, actions: PublicationActions, modifier: Modifier = Modifier) {
    if (state.publishFailed) {
        PublishFailedContent(isRetrying = state.isPublishing, onRetry = actions.onRetryPublish, modifier = modifier)
        return
    }
    ConsultationStepScaffold(
        step = ConsultationStep.PUBLICATION,
        load = state.load.copy(isOffline = state.isOffline || state.load.isOffline),
        onBack = actions.onBack,
        onRetry = actions.onRetry,
        modifier = modifier,
        actions = {
            HealthifyButton(
                text = stringResource(R.string.publication_publish),
                onClick = actions.onPublish,
                loading = state.isPublishing,
                enabled = !state.isOffline && !state.messageTooLong,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        RestrictionsCard(state, actions)
        GuidelinesCard(state, actions)
        MessageCard(state, actions)
        Text(
            text = stringResource(R.string.publication_patient_sees),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestrictionsCard(state: PublicationUiState, actions: PublicationActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(verticalSpacing = dimens.space8) {
        Text(text = stringResource(R.string.publication_restrictions), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        Text(
            text = stringResource(R.string.publication_choose_several),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            PlanRestrictionCode.entries.forEach { code ->
                HealthifyFilterChip(
                    text = stringResource(code.labelRes()),
                    selected = code in state.restrictions,
                    onSelectedChange = { actions.onRestrictionToggle(code) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuidelinesCard(state: PublicationUiState, actions: PublicationActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(verticalSpacing = dimens.space8) {
        Text(text = stringResource(R.string.publication_guidelines), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        if (state.suggested.isNotEmpty()) {
            if (state.suggestedByAi) {
                AiBadge(label = stringResource(R.string.publication_suggested_ai))
            } else {
                HealthifyTag(text = stringResource(R.string.publication_suggested_rule))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            // Las sugeridas van primero; luego el resto del catálogo.
            val ordered = state.suggested + PlanGuidelineCode.entries.filterNot { it in state.suggested }
            ordered.forEach { code ->
                HealthifyFilterChip(
                    text = stringResource(code.labelRes()),
                    selected = code in state.guidelines,
                    onSelectedChange = { actions.onGuidelineToggle(code) },
                )
            }
            // «Otra indicación» escrita por el profesional: se muestra tal cual; tocarla la quita.
            state.customGuidelines.forEach { text ->
                HealthifyFilterChip(text = text, selected = true, onSelectedChange = { actions.onRemoveCustom(text) })
            }
        }
        if (state.showCustomField) {
            HealthifyTextField(
                value = state.customDraft,
                onValueChange = actions.onCustomDraftChange,
                label = stringResource(R.string.publication_other_guideline),
                supportingText = stringResource(R.string.text_counter, state.customDraft.trim().length, CustomGuideline.MAX_LENGTH),
                errorText = if (state.customError) {
                    stringResource(
                        if (state.customGuidelines.size >= CustomGuideline.MAX_PER_VERSION) {
                            R.string.publication_other_guideline_max
                        } else {
                            R.string.publication_other_guideline_length
                        },
                    )
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            HealthifyButton(
                text = stringResource(R.string.publication_add_guideline),
                onClick = actions.onConfirmCustom,
                style = HealthifyButtonStyle.Tonal,
            )
        } else if (state.customGuidelines.size < CustomGuideline.MAX_PER_VERSION) {
            HealthifyButton(
                text = stringResource(R.string.publication_other_guideline),
                onClick = actions.onAddCustomClick,
                style = HealthifyButtonStyle.Text,
                icon = HealthifyIcons.Add,
            )
        }
    }
}

/** Mensaje opcional para el paciente (NC-9): lo verá con sus metas; es texto del profesional y no se traduce. */
@Composable
private fun MessageCard(state: PublicationUiState, actions: PublicationActions) {
    val scheme = MaterialTheme.colorScheme
    SectionCard {
        Text(text = stringResource(R.string.publication_message), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        HealthifyTextField(
            value = state.patientMessage,
            onValueChange = actions.onPatientMessageChange,
            label = stringResource(R.string.publication_message_label),
            placeholder = stringResource(R.string.publication_message_placeholder),
            supportingText = stringResource(R.string.text_counter, state.patientMessage.trim().length, PatientMessage.MAX_LENGTH),
            errorText = if (state.messageTooLong) stringResource(R.string.publication_message_too_long) else null,
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** EV-5.E · «HUBO UN ERROR · No pudimos publicar el plan. Lo que escribiste no se perdió.» (sin culpar a nadie). */
@Composable
private fun PublishFailedContent(isRetrying: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
            .padding(horizontal = dimens.space16, vertical = dimens.space40),
        contentAlignment = Alignment.Center,
    ) {
        FailureState(
            title = stringResource(R.string.publication_error_title),
            text = stringResource(R.string.publication_error_body),
            actionLabel = stringResource(R.string.publication_error_retry),
            onAction = onRetry,
            actionLoading = isRetrying,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "EV-5 · Indicaciones y publicación", widthDp = 360, heightDp = 800)
@Composable
private fun PublicationStepPreview() {
    HealthifyTheme {
        PublicationStepContent(
            state = PublicationUiState(
                load = StepLoadState(isLoading = false),
                restrictions = setOf(PlanRestrictionCode.SHELLFISH_FREE),
                guidelines = setOf(
                    PlanGuidelineCode.PRIORITIZE_VEGETABLES,
                    PlanGuidelineCode.AVOID_SUGARY_DRINKS,
                    PlanGuidelineCode.REDUCE_SALT,
                ),
                suggested = listOf(
                    PlanGuidelineCode.PRIORITIZE_VEGETABLES,
                    PlanGuidelineCode.AVOID_SUGARY_DRINKS,
                    PlanGuidelineCode.REDUCE_SALT,
                ),
                suggestedByAi = true,
                customGuidelines = listOf("Caminar 20 minutos después de cenar"),
            ),
            actions = PublicationActions(),
        )
    }
}

@Preview(name = "EV-5.E · Error al publicar", widthDp = 360, heightDp = 800)
@Composable
private fun PublicationFailedPreview() {
    HealthifyTheme {
        PublicationStepContent(
            state = PublicationUiState(load = StepLoadState(isLoading = false), publishFailed = true),
            actions = PublicationActions(),
        )
    }
}

@Preview(name = "EV-5 · Otra indicación", widthDp = 360, heightDp = 800)
@Composable
private fun PublicationCustomPreview() {
    HealthifyTheme {
        PublicationStepContent(
            state = PublicationUiState(
                load = StepLoadState(isLoading = false),
                showCustomField = true,
                customDraft = "Ca",
                customError = true,
            ),
            actions = PublicationActions(),
        )
    }
}
