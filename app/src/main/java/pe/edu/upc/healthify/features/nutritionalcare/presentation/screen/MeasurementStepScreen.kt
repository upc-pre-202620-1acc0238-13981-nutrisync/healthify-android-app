package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.CardDivider
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyIconDialog
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.IconInfoRow
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SectionOverline
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.mediumDateText
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.FieldProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementForm
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.ConsultationStepScaffold
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.checkInLines
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.conditionsInline
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.decimalText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.headline
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MeasurementUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.ConsultationStepEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.MeasurementStepViewModel
import java.time.Instant

/** Acciones de EV-2 (agrupadas para no pasar veinte lambdas sueltas). */
data class MeasurementActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onEditBaseline: () -> Unit = {},
    val onWeightChange: (String) -> Unit = {},
    val onWaistChange: (String) -> Unit = {},
    val onBodyFatChange: (String) -> Unit = {},
    val onProtocolToggle: (ProtocolCheck) -> Unit = {},
    val onActivityChange: (ActivityLevel) -> Unit = {},
    val onToggleHabits: () -> Unit = {},
    val onToggleBiochemistry: () -> Unit = {},
    val onMealsPerDayChange: (String) -> Unit = {},
    val onWaterChange: (String) -> Unit = {},
    val onMealsOutChange: (String) -> Unit = {},
    val onGlucoseChange: (String) -> Unit = {},
    val onCholesterolChange: (String) -> Unit = {},
    val onTriglyceridesChange: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
)

/**
 * EV-2 · Paso 1 Medición de hoy (+ EV-2.S al tocar ‹). Muestra lo que el paciente contó antes de la consulta (MA-4), los
 * datos base (la talla no se vuelve a pedir), el IMC calculado en vivo, el protocolo con casillas y la actividad; hábitos
 * y bioquímicos son opcionales.
 */
@Composable
fun MeasurementStepScreen(
    onExit: () -> Unit,
    onContinue: (patientId: Long, patientName: String) -> Unit,
    onEditBaseline: (patientId: Long, patientName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    BackHandler(onBack = viewModel::onBack)
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            ConsultationStepEvent.Continue -> onContinue(viewModel.patientId, viewModel.patientName)
            ConsultationStepEvent.Exit, ConsultationStepEvent.ConsultationClosed, ConsultationStepEvent.Back -> onExit()
            ConsultationStepEvent.Published -> Unit
        }
    }
    MeasurementStepContent(
        state = state,
        patientName = viewModel.patientName,
        actions = MeasurementActions(
            onBack = viewModel::onBack,
            onRetry = viewModel::onRetry,
            onEditBaseline = { onEditBaseline(viewModel.patientId, viewModel.patientName) },
            onWeightChange = viewModel::onWeightChange,
            onWaistChange = viewModel::onWaistChange,
            onBodyFatChange = viewModel::onBodyFatChange,
            onProtocolToggle = viewModel::onProtocolToggle,
            onActivityChange = viewModel::onActivityChange,
            onToggleHabits = viewModel::onToggleHabits,
            onToggleBiochemistry = viewModel::onToggleBiochemistry,
            onMealsPerDayChange = viewModel::onMealsPerDayChange,
            onWaterChange = viewModel::onWaterChange,
            onMealsOutChange = viewModel::onMealsOutChange,
            onGlucoseChange = viewModel::onGlucoseChange,
            onCholesterolChange = viewModel::onCholesterolChange,
            onTriglyceridesChange = viewModel::onTriglyceridesChange,
            onContinue = viewModel::onContinue,
        ),
        modifier = modifier,
    )
    if (state.showExitDialog) {
        // EV-2.S · ¿Salir de la consulta?
        HealthifyIconDialog(
            icon = HealthifyIcons.Warning,
            title = stringResource(R.string.consultation_exit_title),
            text = stringResource(R.string.consultation_exit_body, viewModel.patientName),
            confirmLabel = stringResource(R.string.consultation_exit_confirm),
            onConfirm = viewModel::onExit,
            dismissLabel = stringResource(R.string.consultation_exit_stay),
            onDismiss = viewModel::onStay,
        )
    }
    if (state.saveFailure != null) {
        ServerErrorDialog(onRetry = viewModel::onContinue, onDismiss = viewModel::onSaveFailureDismissed)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementStepContent(
    state: MeasurementUiState,
    patientName: String,
    actions: MeasurementActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    ConsultationStepScaffold(
        step = ConsultationStep.MEASUREMENT,
        load = state.load,
        onBack = actions.onBack,
        onRetry = actions.onRetry,
        subtitle = stringResource(R.string.measurement_subtitle),
        modifier = modifier,
        actions = {
            HealthifyButton(
                text = stringResource(R.string.measurement_continue),
                onClick = actions.onContinue,
                loading = state.isSaving,
                enabled = !state.load.isOffline,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        state.checkIn?.let { CheckInCard(it, patientName) }
        state.baseline?.let { BaselineRowCard(it, actions.onEditBaseline, enabled = !state.load.isOffline) }
        MeasuresCard(state, actions)
        SectionCard(verticalSpacing = dimens.space8) {
            Text(
                text = stringResource(R.string.measurement_protocol),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                ProtocolCheck.entries.forEach { check ->
                    HealthifyFilterChip(
                        text = stringResource(check.labelRes()),
                        selected = check in state.form.protocolChecks,
                        onSelectedChange = { actions.onProtocolToggle(check) },
                    )
                }
            }
            if (state.problems[MeasurementField.PROTOCOL] != null) {
                ErrorLine(stringResource(R.string.measurement_protocol_required))
            }
        }
        SectionCard(verticalSpacing = dimens.space8) {
            Text(
                text = stringResource(R.string.measurement_activity),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                verticalArrangement = Arrangement.spacedBy(dimens.space8),
            ) {
                ActivityLevel.entries.forEach { level ->
                    HealthifyFilterChip(
                        text = stringResource(level.labelRes()),
                        selected = state.form.activityLevel == level,
                        onSelectedChange = { actions.onActivityChange(level) },
                    )
                }
            }
            if (state.problems[MeasurementField.ACTIVITY] != null) {
                ErrorLine(stringResource(R.string.measurement_activity_required))
            }
        }
        OptionalSection(state, actions)
    }
}

/** «Antes de la consulta, Ana contó» (MA-4). Las preguntas son del paciente: se muestran tal cual. */
@Composable
private fun CheckInCard(checkIn: PatientCheckIn, patientName: String) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    val firstName = patientName.trim().substringBefore(' ').ifBlank { patientName }
    SectionCard(verticalSpacing = dimens.space8) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = HealthifyIcons.Help, contentDescription = null, tint = scheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.measurement_check_in_title, firstName),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
        }
        checkInLines(checkIn).forEach { line ->
            Text(
                text = stringResource(R.string.bullet_line, line),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.measurement_check_in_sent, checkIn.submittedAt.mediumDateText()),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** «Mujer · 31 años · 168 cm · Hipotiroidismo · de los datos base · Editar». */
@Composable
private fun BaselineRowCard(baseline: BaselineSummary, onEdit: () -> Unit, enabled: Boolean) {
    val conditions = baseline.conditionsInline()
    SectionCard {
        IconInfoRow(
            icon = HealthifyIcons.Person,
            title = baseline.headline(),
            supportingText = conditions?.let {
                stringResource(R.string.measurement_from_baseline_with, it.replaceFirstChar { c -> c.titlecase() })
            } ?: stringResource(R.string.measurement_from_baseline),
            trailing = {
                HealthifyButton(
                    text = stringResource(R.string.common_edit),
                    onClick = onEdit,
                    style = HealthifyButtonStyle.Text,
                    enabled = enabled,
                )
            },
        )
    }
}

@Composable
private fun MeasuresCard(state: MeasurementUiState, actions: MeasurementActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(verticalSpacing = dimens.space16) {
        Text(
            text = stringResource(R.string.measurement_measures),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
        )
        NumberField(
            value = state.form.weight,
            onValueChange = actions.onWeightChange,
            label = stringResource(R.string.measurement_weight),
            unit = stringResource(R.string.unit_kg),
            problem = state.problems[MeasurementField.WEIGHT],
            rangeError = stringResource(R.string.measurement_weight_range),
            requiredError = stringResource(R.string.measurement_weight_required),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.space8)) {
            NumberField(
                value = state.form.waist,
                onValueChange = actions.onWaistChange,
                label = stringResource(R.string.measurement_waist),
                unit = stringResource(R.string.unit_cm),
                problem = state.problems[MeasurementField.WAIST],
                rangeError = stringResource(R.string.measurement_waist_range),
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = state.form.bodyFat,
                onValueChange = actions.onBodyFatChange,
                label = stringResource(R.string.measurement_body_fat),
                unit = stringResource(R.string.unit_percent),
                placeholder = stringResource(R.string.measurement_body_fat_placeholder),
                problem = state.problems[MeasurementField.BODY_FAT],
                rangeError = stringResource(R.string.measurement_body_fat_range),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.measurement_optional_note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        BmiResult(state.bmi)
    }
}

/** «IMC calculado · 26.3 kg/m²» (`surfaceContainer`, radio 12). Sin peso válido: «—». */
@Composable
private fun BmiResult(bmi: Double?) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainer)
            .padding(horizontal = dimens.space12, vertical = dimens.space8),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = HealthifyIcons.Info, contentDescription = null, tint = scheme.onSurfaceVariant)
        Text(
            text = stringResource(R.string.measurement_bmi),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = bmi?.let { stringResource(R.string.unit_bmi_value, decimalText(it)) } ?: stringResource(R.string.value_missing),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
        )
    }
}

/** «OPCIONAL»: hábitos alimentarios y datos bioquímicos se despliegan con «+». */
@Composable
private fun OptionalSection(state: MeasurementUiState, actions: MeasurementActions) {
    val dimens = HealthifyTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        SectionOverline(stringResource(R.string.measurement_optional))
        SectionCard {
            ExpandableRow(
                title = stringResource(R.string.measurement_habits),
                supporting = stringResource(R.string.measurement_habits_hint),
                expanded = state.habitsExpanded,
                onToggle = actions.onToggleHabits,
            )
            if (state.habitsExpanded) {
                NumberField(
                    value = state.form.mealsPerDay,
                    onValueChange = actions.onMealsPerDayChange,
                    label = stringResource(R.string.measurement_meals_per_day),
                    problem = state.problems[MeasurementField.MEALS_PER_DAY],
                    rangeError = stringResource(R.string.measurement_meals_per_day_range),
                    whole = true,
                )
                NumberField(
                    value = state.form.waterLiters,
                    onValueChange = actions.onWaterChange,
                    label = stringResource(R.string.measurement_water),
                    unit = stringResource(R.string.unit_liters_per_day),
                    problem = state.problems[MeasurementField.WATER],
                    rangeError = stringResource(R.string.measurement_water_range),
                )
                NumberField(
                    value = state.form.mealsOut,
                    onValueChange = actions.onMealsOutChange,
                    label = stringResource(R.string.measurement_meals_out),
                    problem = state.problems[MeasurementField.MEALS_OUT],
                    rangeError = stringResource(R.string.measurement_meals_out_range),
                    whole = true,
                )
            }
            CardDivider()
            ExpandableRow(
                title = stringResource(R.string.measurement_biochemistry),
                supporting = stringResource(R.string.measurement_biochemistry_hint),
                expanded = state.biochemistryExpanded,
                onToggle = actions.onToggleBiochemistry,
            )
            if (state.biochemistryExpanded) {
                NumberField(
                    value = state.form.glucose,
                    onValueChange = actions.onGlucoseChange,
                    label = stringResource(R.string.measurement_glucose),
                    unit = stringResource(R.string.unit_mg_dl),
                    problem = state.problems[MeasurementField.GLUCOSE],
                    rangeError = stringResource(R.string.measurement_glucose_range),
                )
                NumberField(
                    value = state.form.cholesterol,
                    onValueChange = actions.onCholesterolChange,
                    label = stringResource(R.string.measurement_cholesterol),
                    unit = stringResource(R.string.unit_mg_dl),
                    problem = state.problems[MeasurementField.CHOLESTEROL],
                    rangeError = stringResource(R.string.measurement_cholesterol_range),
                )
                NumberField(
                    value = state.form.triglycerides,
                    onValueChange = actions.onTriglyceridesChange,
                    label = stringResource(R.string.measurement_triglycerides),
                    unit = stringResource(R.string.unit_mg_dl),
                    problem = state.problems[MeasurementField.TRIGLYCERIDES],
                    rangeError = stringResource(R.string.measurement_triglycerides_range),
                )
            }
        }
    }
}

@Composable
private fun ExpandableRow(title: String, supporting: String, expanded: Boolean, onToggle: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.touchMin)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(dimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.space4)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Icon(
            imageVector = if (expanded) HealthifyIcons.Close else HealthifyIcons.Add,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
        )
    }
}

/** Campo numérico con su unidad y el error de rango de las Notas. */
@Composable
internal fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    problem: FieldProblem?,
    rangeError: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    placeholder: String? = null,
    requiredError: String? = null,
    whole: Boolean = false,
) {
    HealthifyTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        type = if (whole) HealthifyTextFieldType.Number else HealthifyTextFieldType.Decimal,
        errorText = when (problem) {
            FieldProblem.REQUIRED -> requiredError ?: rangeError
            FieldProblem.OUT_OF_RANGE -> rangeError
            null -> null
        },
        trailingIcon = unit?.let { { UnitSuffix(it) } },
        modifier = modifier.fillMaxWidth(),
    )
}

private val previewCheckIn = PatientCheckIn(
    feeling = "Fair",
    difficulties = listOf("Dinners", "Weekends"),
    questions = listOf("¿Puedo comer fuera los viernes?", "¿Cómo armo cenas con más proteína?"),
    submittedAt = Instant.parse("2026-09-15T15:00:00Z"),
)

@Preview(name = "EV-2 · Medición de hoy", widthDp = 360, heightDp = 800)
@Composable
private fun MeasurementStepPreview() {
    HealthifyTheme {
        MeasurementStepContent(
            state = MeasurementUiState(
                load = StepLoadState(isLoading = false),
                baseline = BaselineSummary(BiologicalSex.FEMALE, 31, 168.0, setOf(MedicalCondition.HYPOTHYROIDISM)),
                checkIn = previewCheckIn,
                form = MeasurementForm(
                    weight = "74.2",
                    waist = "88",
                    protocolChecks = setOf(ProtocolCheck.FASTING, ProtocolCheck.NO_SHOES, ProtocolCheck.LIGHT_CLOTHING),
                    activityLevel = ActivityLevel.MODERATE,
                ),
                bmi = 26.3,
            ),
            patientName = "Ana Flores",
            actions = MeasurementActions(),
        )
    }
}

@Preview(name = "EV-2 · Datos inválidos", widthDp = 360, heightDp = 800)
@Composable
private fun MeasurementStepErrorsPreview() {
    HealthifyTheme {
        MeasurementStepContent(
            state = MeasurementUiState(
                load = StepLoadState(isLoading = false),
                form = MeasurementForm(weight = "500"),
                problems = mapOf(
                    MeasurementField.WEIGHT to FieldProblem.OUT_OF_RANGE,
                    MeasurementField.PROTOCOL to FieldProblem.REQUIRED,
                    MeasurementField.ACTIVITY to FieldProblem.REQUIRED,
                ),
            ),
            patientName = "Ana Flores",
            actions = MeasurementActions(),
        )
    }
}

@Preview(name = "EV-2 · Cargando", widthDp = 360, heightDp = 800)
@Composable
private fun MeasurementStepLoadingPreview() {
    HealthifyTheme {
        MeasurementStepContent(state = MeasurementUiState(), patientName = "Ana Flores", actions = MeasurementActions())
    }
}
