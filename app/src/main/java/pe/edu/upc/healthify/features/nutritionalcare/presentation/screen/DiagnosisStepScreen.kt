package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTag
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ConsultationStepScaffold
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.DiagnosisSuggestionUi
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.DiagnosisUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ConsultationStepEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.DiagnosisStepViewModel

/**
 * EV-3 · Paso 2 Diagnóstico: «Solo para tu expediente profesional. El paciente no lo verá.» Sugerencia de IA (o por
 * regla) con su fundamento, «Usar sugerencia» / «Elegir otro» y la lista cerrada de diagnósticos (sin «Otro»).
 */
@Composable
fun DiagnosisStepScreen(
    onBack: (patientId: Long, patientName: String) -> Unit,
    onContinue: (patientId: Long, patientName: String) -> Unit,
    onConsultationClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosisStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back = { onBack(viewModel.patientId, viewModel.patientName) }
    BackHandler(onBack = back)
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ConsultationStepEvent.Continue -> onContinue(viewModel.patientId, viewModel.patientName)
            ConsultationStepEvent.Back -> back()
            ConsultationStepEvent.ConsultationClosed, ConsultationStepEvent.Exit -> onConsultationClosed()
            ConsultationStepEvent.Published -> Unit
        }
    }
    DiagnosisStepContent(
        state = state,
        onBack = back,
        onRetry = viewModel::onRetry,
        onUseSuggestion = viewModel::onUseSuggestion,
        onChooseOther = viewModel::onChooseOther,
        onSelect = viewModel::onSelect,
        onContinue = viewModel::onContinue,
        modifier = modifier,
    )
    if (state.saveFailure != null) {
        ServerErrorDialog(onRetry = viewModel::onContinue, onDismiss = viewModel::onSaveFailureDismissed)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosisStepContent(
    state: DiagnosisUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onUseSuggestion: () -> Unit,
    onChooseOther: () -> Unit,
    onSelect: (DiagnosisCode) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    ConsultationStepScaffold(
        step = ConsultationStep.DIAGNOSIS,
        load = state.load,
        onBack = onBack,
        onRetry = onRetry,
        subtitle = stringResource(R.string.diagnosis_subtitle),
        modifier = modifier,
        actions = {
            HealthifyButton(
                text = stringResource(R.string.diagnosis_continue),
                onClick = onContinue,
                loading = state.isSaving,
                enabled = !state.load.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        when {
            state.suggestion != null -> SuggestionCard(state.suggestion, state.usesSuggestion, onUseSuggestion, onChooseOther)
            state.isSuggesting -> SkeletonCard(modifier = Modifier.fillMaxWidth())
        }
        SectionCard(verticalSpacing = dimens.space8) {
            Text(
                text = stringResource(R.string.diagnosis_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                DiagnosisCode.entries.forEach { code ->
                    HealthifyFilterChip(
                        text = stringResource(code.labelRes()),
                        selected = state.selected == code,
                        onSelectedChange = { onSelect(code) },
                    )
                }
            }
            if (state.showRequired) ErrorLine(stringResource(R.string.diagnosis_required))
        }
    }
}

/**
 * «Sugerencia de IA · Sobrepeso grado I · Fundamento clínico…» (borde `tertiary`). La de regla (sin IA) dice «Sugerencia
 * según el IMC»: nunca se rotula IA lo que no lo es.
 */
@Composable
private fun SuggestionCard(
    suggestion: DiagnosisSuggestionUi,
    used: Boolean,
    onUseSuggestion: () -> Unit,
    onChooseOther: () -> Unit,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(borderColor = scheme.tertiary) {
        if (suggestion.isFromAi) {
            AiBadge(label = stringResource(R.string.diagnosis_ai_badge))
        } else {
            HealthifyTag(text = stringResource(R.string.diagnosis_rule_badge))
        }
        Text(
            text = stringResource(suggestion.code.labelRes()),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        if (suggestion.rationale.isNotBlank()) {
            Text(
                text = stringResource(R.string.diagnosis_rationale),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
            )
            // El fundamento de la IA ya llega en el idioma del lector; no se traduce.
            Text(text = suggestion.rationale, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        }
        Text(
            text = suggestion.disclaimer.ifBlank { stringResource(R.string.diagnosis_disclaimer) },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space12), verticalAlignment = Alignment.CenterVertically) {
            HealthifyButton(
                text = stringResource(if (used) R.string.diagnosis_suggestion_used else R.string.diagnosis_use_suggestion),
                onClick = onUseSuggestion,
                style = HealthifyButtonStyle.Tonal,
            )
            HealthifyButton(
                text = stringResource(R.string.diagnosis_choose_other),
                onClick = onChooseOther,
                style = HealthifyButtonStyle.Text,
            )
        }
    }
}

private val previewSuggestion = DiagnosisSuggestionUi(
    code = DiagnosisCode.OVERWEIGHT_GRADE_I,
    rationale = "IMC 26.3 kg/m² (rango 25 a 29.9). Cintura de 88 cm: riesgo cardiometabólico aumentado.",
    isFromAi = true,
    disclaimer = "Se basa en la medición y la historia de esta evaluación. Revísala antes de usarla.",
)

@Preview(name = "EV-3 · Diagnóstico", widthDp = 360, heightDp = 800)
@Composable
private fun DiagnosisStepPreview() {
    HealthifyTheme {
        DiagnosisStepContent(
            state = DiagnosisUiState(
                load = StepLoadState(isLoading = false),
                suggestion = previewSuggestion,
                selected = DiagnosisCode.OVERWEIGHT_GRADE_I,
                usesSuggestion = true,
            ),
            onBack = {},
            onRetry = {},
            onUseSuggestion = {},
            onChooseOther = {},
            onSelect = {},
            onContinue = {},
        )
    }
}

@Preview(name = "EV-3 · Sugerencia por regla, sin elegir", widthDp = 360, heightDp = 800)
@Composable
private fun DiagnosisStepRulePreview() {
    HealthifyTheme {
        DiagnosisStepContent(
            state = DiagnosisUiState(
                load = StepLoadState(isLoading = false),
                suggestion = previewSuggestion.copy(isFromAi = false, rationale = ""),
                showRequired = true,
            ),
            onBack = {},
            onRetry = {},
            onUseSuggestion = {},
            onChooseOther = {},
            onSelect = {},
            onContinue = {},
        )
    }
}
