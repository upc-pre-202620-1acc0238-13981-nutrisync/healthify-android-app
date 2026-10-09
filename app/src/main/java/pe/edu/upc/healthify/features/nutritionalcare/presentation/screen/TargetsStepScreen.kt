package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.ErrorState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.IconInfoRow
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SegmentedSelector
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.CalculationBasis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetInputs
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.OverrideReason
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ConsultationStepScaffold
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.compactText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.deficitText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.inlineRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.summaryRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.OverrideForm
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.ParametersForm
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.TargetsUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ConsultationStepEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.TargetsStepViewModel

/** Acciones de EV-4. */
data class TargetsActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onChangeParameters: () -> Unit = {},
    val onParametersChange: (ParametersForm) -> Unit = {},
    val onApplyParameters: () -> Unit = {},
    val onParametersDismissed: () -> Unit = {},
    val onAccept: () -> Unit = {},
    val onWriteOwnValues: () -> Unit = {},
    val onOverrideChange: (OverrideForm) -> Unit = {},
    val onSaveOverride: () -> Unit = {},
    val onOverrideDismissed: () -> Unit = {},
)

/**
 * EV-4 · Paso 3 Metas calculadas: «Calculado con…», la propuesta (energía y macros con su parte de la energía) y el
 * método; «Cambiar parámetros», «Aceptar metas» y «Escribir mis propios valores» (con razón obligatoria).
 */
@Composable
fun TargetsStepScreen(
    onBack: (patientId: Long, patientName: String) -> Unit,
    onContinue: (patientId: Long, patientName: String) -> Unit,
    onConsultationClosed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TargetsStepViewModel = hiltViewModel(),
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
    val actions = TargetsActions(
        onBack = back,
        onRetry = viewModel::onRetry,
        onChangeParameters = viewModel::onChangeParameters,
        onParametersChange = viewModel::onParametersChange,
        onApplyParameters = viewModel::onApplyParameters,
        onParametersDismissed = viewModel::onParametersDismissed,
        onAccept = viewModel::onAccept,
        onWriteOwnValues = viewModel::onWriteOwnValues,
        onOverrideChange = viewModel::onOverrideChange,
        onSaveOverride = viewModel::onSaveOverride,
        onOverrideDismissed = viewModel::onOverrideDismissed,
    )
    TargetsStepContent(state = state, actions = actions, modifier = modifier)
    if (state.showParameters) {
        state.parameters?.let { form ->
            HealthifyBottomSheet(onDismissRequest = actions.onParametersDismissed) {
                ParametersSheet(form, state.parametersInvalid, actions)
            }
        }
    }
    if (state.showOverride) {
        HealthifyBottomSheet(onDismissRequest = actions.onOverrideDismissed) {
            OverrideSheet(state.override, state.overrideProblems, state.isSaving, actions)
        }
    }
    if (state.saveFailure != null) {
        ServerErrorDialog(onRetry = viewModel::onRetrySave, onDismiss = viewModel::onSaveFailureDismissed)
    }
}

@Composable
fun TargetsStepContent(state: TargetsUiState, actions: TargetsActions, modifier: Modifier = Modifier) {
    ConsultationStepScaffold(
        step = ConsultationStep.TARGETS,
        load = state.load,
        onBack = actions.onBack,
        onRetry = actions.onRetry,
        subtitle = stringResource(R.string.targets_subtitle),
        modifier = modifier,
        actions = {
            if (state.proposal != null) {
                HealthifyButton(
                    text = stringResource(R.string.targets_accept),
                    onClick = actions.onAccept,
                    loading = state.isSaving && !state.showOverride,
                    enabled = !state.load.isOffline && !state.isCalculating,
                    modifier = Modifier.fillMaxWidth(),
                )
                HealthifyButton(
                    text = stringResource(R.string.targets_write_own),
                    onClick = actions.onWriteOwnValues,
                    style = HealthifyButtonStyle.Text,
                    enabled = !state.load.isOffline && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        val proposal = state.proposal
        when {
            proposal != null -> {
                InputsCard(proposal.inputs)
                ProposalCard(proposal, state.isCalculating, actions.onChangeParameters, enabled = !state.load.isOffline)
            }
            state.isCalculating -> SkeletonCard(modifier = Modifier.fillMaxWidth())
            state.proposalFailed -> ErrorState(onRetry = actions.onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** «Calculado con · Mujer · 31 años · 168 cm · 74.2 kg · actividad moderada». */
@Composable
private fun InputsCard(inputs: TargetInputs) {
    val parts = listOfNotNull(
        inputs.sex?.let { stringResource(it.summaryRes()) },
        androidx.compose.ui.res.pluralStringResource(R.plurals.baseline_age_years, inputs.ageYears, inputs.ageYears),
        stringResource(R.string.unit_cm_value, compactText(inputs.heightCm)),
        stringResource(R.string.unit_kg_value, compactText(inputs.weightKg, 2)),
        inputs.activityLevel?.let { stringResource(it.inlineRes()) },
    )
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Info,
            title = stringResource(R.string.targets_calculated_with),
            supportingText = parts.joinToString(stringResource(R.string.text_separator)),
        )
    }
}

@Composable
private fun ProposalCard(proposal: TargetProposal, isCalculating: Boolean, onChangeParameters: () -> Unit, enabled: Boolean) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val targets = proposal.proposal
    SectionCard(verticalSpacing = dimens.space16) {
        Text(text = stringResource(R.string.targets_proposed), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = compactText(targets.energyKcal, 0),
                style = HealthifyTheme.extendedTypography.metricLarge,
                color = scheme.onSurface,
            )
            Text(text = stringResource(R.string.targets_kcal_per_day), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            MacroBox(targets.proteinG, stringResource(R.string.macro_protein), targets.proteinPercent, Modifier.weight(1f))
            MacroBox(targets.carbG, stringResource(R.string.macro_carbs_short), targets.carbPercent, Modifier.weight(1f))
            MacroBox(targets.fatG, stringResource(R.string.macro_fat), targets.fatPercent, Modifier.weight(1f))
        }
        Text(text = methodText(proposal.basis), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        HealthifyButton(
            text = stringResource(R.string.targets_change_parameters),
            onClick = onChangeParameters,
            style = HealthifyButtonStyle.Text,
            icon = HealthifyIcons.Edit,
            loading = isCalculating,
            enabled = enabled,
        )
    }
}

@Composable
private fun MacroBox(grams: Double, label: String, percent: Int, modifier: Modifier) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainer)
            .padding(horizontal = dimens.space8, vertical = dimens.space12),
        verticalArrangement = Arrangement.spacedBy(dimens.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.unit_g_value, compactText(grams, 0)),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.targets_macro_share, label, percent),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** «Mifflin-St Jeor · déficit de 500 kcal · factor de actividad 1,55». */
@Composable
private fun methodText(basis: CalculationBasis): String {
    val parts = listOfNotNull(
        basis.equation?.let { stringResource(it.labelRes()) },
        deficitText(basis.deficitKind, basis.deficitValue),
        stringResource(R.string.targets_activity_factor, compactText(basis.activityFactor, 3)),
    )
    return parts.joinToString(stringResource(R.string.text_separator))
}

/** «Cambiar parámetros»: ecuación, déficit, proteína por kg y grasa (el backend recalcula el mismo borrador). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParametersSheet(form: ParametersForm, invalid: Boolean, actions: TargetsActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Text(text = stringResource(R.string.targets_parameters_title), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        FieldTitle(stringResource(R.string.targets_equation))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            EnergyEquation.entries.forEach { equation ->
                HealthifyFilterChip(
                    text = stringResource(equation.labelRes()),
                    selected = form.equation == equation,
                    onSelectedChange = { actions.onParametersChange(form.copy(equation = equation)) },
                )
            }
        }
        FieldTitle(stringResource(R.string.targets_deficit))
        SegmentedSelector(
            options = DeficitKind.entries,
            selected = form.deficitKind,
            onSelect = { actions.onParametersChange(form.copy(deficitKind = it)) },
            optionLabel = {
                stringResource(if (it == DeficitKind.FIXED_KCAL) R.string.targets_deficit_kind_kcal else R.string.targets_deficit_kind_percent)
            },
        )
        HealthifyTextField(
            value = form.deficit,
            onValueChange = { actions.onParametersChange(form.copy(deficit = it)) },
            label = stringResource(R.string.targets_deficit_value),
            type = HealthifyTextFieldType.Decimal,
            trailingIcon = {
                UnitSuffix(stringResource(if (form.deficitKind == DeficitKind.FIXED_KCAL) R.string.unit_kcal else R.string.unit_percent))
            },
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyTextField(
            value = form.proteinPerKg,
            onValueChange = { actions.onParametersChange(form.copy(proteinPerKg = it)) },
            label = stringResource(R.string.targets_protein_per_kg),
            type = HealthifyTextFieldType.Decimal,
            trailingIcon = { UnitSuffix(stringResource(R.string.unit_g_per_kg)) },
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyTextField(
            value = form.fatPercent,
            onValueChange = { actions.onParametersChange(form.copy(fatPercent = it)) },
            label = stringResource(R.string.targets_fat_percent),
            type = HealthifyTextFieldType.Decimal,
            trailingIcon = { UnitSuffix(stringResource(R.string.unit_percent)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (invalid) ErrorLine(stringResource(R.string.targets_parameters_invalid))
        HealthifyButton(
            text = stringResource(R.string.targets_recalculate),
            onClick = actions.onApplyParameters,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.ds_cancel),
            onClick = actions.onParametersDismissed,
            style = HealthifyButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** «Escribir mis propios valores»: energía y macros, y la razón (obligatoria, hasta 500 caracteres). */
@Composable
private fun OverrideSheet(form: OverrideForm, problems: Set<OverrideField>, isSaving: Boolean, actions: TargetsActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space16),
    ) {
        Text(text = stringResource(R.string.targets_override_title), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        HealthifyTextField(
            value = form.energy,
            onValueChange = { actions.onOverrideChange(form.copy(energy = it)) },
            label = stringResource(R.string.targets_energy),
            type = HealthifyTextFieldType.Decimal,
            errorText = if (OverrideField.ENERGY in problems) stringResource(R.string.targets_energy_invalid) else null,
            trailingIcon = { UnitSuffix(stringResource(R.string.unit_kcal)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.Top) {
            GramsField(form.protein, stringResource(R.string.macro_protein), OverrideField.PROTEIN in problems, Modifier.weight(1f)) {
                actions.onOverrideChange(form.copy(protein = it))
            }
            GramsField(form.carb, stringResource(R.string.macro_carbs_short), OverrideField.CARB in problems, Modifier.weight(1f)) {
                actions.onOverrideChange(form.copy(carb = it))
            }
            GramsField(form.fat, stringResource(R.string.macro_fat), OverrideField.FAT in problems, Modifier.weight(1f)) {
                actions.onOverrideChange(form.copy(fat = it))
            }
        }
        HealthifyTextField(
            value = form.reason,
            onValueChange = { actions.onOverrideChange(form.copy(reason = it)) },
            label = stringResource(R.string.targets_override_reason),
            placeholder = stringResource(R.string.targets_override_reason_placeholder),
            supportingText = stringResource(R.string.text_counter, form.reason.trim().length, OverrideReason.MAX_LENGTH),
            errorText = if (OverrideField.REASON in problems) stringResource(R.string.targets_override_reason_required) else null,
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.targets_override_save),
            onClick = actions.onSaveOverride,
            loading = isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        HealthifyButton(
            text = stringResource(R.string.ds_cancel),
            onClick = actions.onOverrideDismissed,
            style = HealthifyButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GramsField(value: String, label: String, isError: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    HealthifyTextField(
        value = value,
        onValueChange = onChange,
        label = label,
        type = HealthifyTextFieldType.Decimal,
        isError = isError,
        trailingIcon = { UnitSuffix(stringResource(R.string.unit_g)) },
        modifier = modifier,
    )
}

private val previewProposal = TargetProposal(
    basis = CalculationBasis(EnergyEquation.MIFFLIN_ST_JEOR, 74.2, 1.55, DeficitKind.FIXED_KCAL, 500.0),
    proposal = Targets(1796.0, 119.0, 195.0, 60.0),
    inputs = TargetInputs(BiologicalSex.FEMALE, 31, 168.0, 74.2, ActivityLevel.MODERATE),
)

@Preview(name = "EV-4 · Metas calculadas", widthDp = 360, heightDp = 800)
@Composable
private fun TargetsStepPreview() {
    HealthifyTheme {
        TargetsStepContent(
            state = TargetsUiState(load = StepLoadState(isLoading = false), proposal = previewProposal),
            actions = TargetsActions(),
        )
    }
}

/** Fuente al 200 %: el contenido se desplaza y las acciones quedan fijas abajo. */
@Preview(name = "EV-4 · Metas calculadas — fuente 200 %", widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun TargetsStepLargeFontPreview() {
    HealthifyTheme {
        TargetsStepContent(
            state = TargetsUiState(load = StepLoadState(isLoading = false), proposal = previewProposal),
            actions = TargetsActions(),
        )
    }
}

@Preview(name = "EV-4 · Calculando", widthDp = 360, heightDp = 800)
@Composable
private fun TargetsStepCalculatingPreview() {
    HealthifyTheme {
        TargetsStepContent(
            state = TargetsUiState(load = StepLoadState(isLoading = false), isCalculating = true),
            actions = TargetsActions(),
        )
    }
}

@Preview(name = "EV-4 · Valores propios sin razón", widthDp = 360, heightDp = 800)
@Composable
private fun TargetsOverridePreview() {
    HealthifyTheme {
        OverrideSheet(
            form = OverrideForm("1700", "120", "180", "55", ""),
            problems = setOf(OverrideField.REASON),
            isSaving = false,
            actions = TargetsActions(),
        )
    }
}
