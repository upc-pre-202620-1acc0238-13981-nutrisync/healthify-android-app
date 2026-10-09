package pe.edu.upc.healthify.features.nutritionalcare.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButtonStyle
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextField
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTextFieldType
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ReadOnlyClickableField
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SegmentedSelector
import pe.edu.upc.healthify.core.designsystem.component.ServerErrorDialog
import pe.edu.upc.healthify.core.designsystem.component.SkeletonCard
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.optionRes
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.BaselineEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.BaselineUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel.BaselineViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * EV-1 · Datos base (solo la primera vez; luego se editan desde Resumen o EV-2). ‹ y «Guardar y salir» vuelven a la
 * pantalla desde donde se abrió.
 */
@Composable
fun BaselineScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onStartConsultation: (step: ConsultationStep, patientId: Long, patientName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BaselineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            BaselineEvent.Saved -> onSaved()
            is BaselineEvent.ConsultationStarted -> onStartConsultation(event.step, viewModel.patientId, viewModel.patientName)
        }
    }
    BaselineContent(
        state = state,
        onBack = onBack,
        onBirthDateClick = viewModel::onBirthDateClick,
        onSexChange = viewModel::onSexChange,
        onHeightChange = viewModel::onHeightChange,
        onConditionToggle = viewModel::onConditionToggle,
        onSaveAndStart = viewModel::onSaveAndStart,
        onSaveAndExit = viewModel::onSaveAndExit,
        modifier = modifier,
    )
    if (state.showDatePicker) {
        BirthDatePicker(
            initial = state.birthDate,
            onConfirm = viewModel::onBirthDateChange,
            onDismiss = viewModel::onDatePickerDismissed,
        )
    }
    if (state.saveFailure != null) {
        ServerErrorDialog(onRetry = viewModel::onRetrySave, onDismiss = viewModel::onSaveFailureDismissed)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaselineContent(
    state: BaselineUiState,
    onBack: () -> Unit,
    onBirthDateClick: () -> Unit,
    onSexChange: (BiologicalSex) -> Unit,
    onHeightChange: (String) -> Unit,
    onConditionToggle: (MedicalCondition) -> Unit,
    onSaveAndStart: () -> Unit,
    onSaveAndExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SystemBarsAppearance(darkBackground = false)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .imePadding(),
    ) {
        HealthifyTopAppBar(title = stringResource(R.string.baseline_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            Text(
                text = stringResource(R.string.baseline_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            if (state.isLoading) {
                SkeletonCard(modifier = Modifier.fillMaxWidth())
                return@Column
            }
            SectionCard(verticalSpacing = dimens.space16) {
                BirthDateField(state, onBirthDateClick)
                Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
                    FieldTitle(stringResource(R.string.baseline_sex))
                    SegmentedSelector(
                        options = BiologicalSex.entries,
                        selected = state.sex,
                        onSelect = onSexChange,
                        optionLabel = { stringResource(it.optionRes()) },
                        isError = BaselineProblem.SEX_REQUIRED in state.problems,
                    )
                    if (BaselineProblem.SEX_REQUIRED in state.problems) {
                        ErrorLine(stringResource(R.string.baseline_sex_required))
                    }
                }
                HealthifyTextField(
                    value = state.heightText,
                    onValueChange = onHeightChange,
                    label = stringResource(R.string.baseline_height),
                    type = HealthifyTextFieldType.Decimal,
                    errorText = when {
                        BaselineProblem.HEIGHT_REQUIRED in state.problems -> stringResource(R.string.baseline_height_required)
                        BaselineProblem.HEIGHT_OUT_OF_RANGE in state.problems -> stringResource(R.string.baseline_height_range)
                        else -> null
                    },
                    trailingIcon = { UnitSuffix(stringResource(R.string.unit_cm)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SectionCard(verticalSpacing = dimens.space8) {
                Text(
                    text = stringResource(R.string.baseline_conditions),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.baseline_conditions_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.space8),
                    verticalArrangement = Arrangement.spacedBy(dimens.space8),
                ) {
                    MedicalCondition.entries.forEach { condition ->
                        HealthifyFilterChip(
                            text = stringResource(condition.labelRes()),
                            selected = condition in state.conditions,
                            onSelectedChange = { onConditionToggle(condition) },
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space12),
        ) {
            if (state.editing) {
                HealthifyButton(
                    text = stringResource(R.string.baseline_save),
                    onClick = onSaveAndExit,
                    loading = state.isSaving,
                    enabled = !state.isOffline && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                HealthifyButton(
                    text = stringResource(R.string.baseline_save_and_start),
                    onClick = onSaveAndStart,
                    loading = state.isSaving,
                    enabled = !state.isOffline,
                    modifier = Modifier.fillMaxWidth(),
                )
                HealthifyButton(
                    text = stringResource(R.string.baseline_save_and_exit),
                    onClick = onSaveAndExit,
                    style = HealthifyButtonStyle.Text,
                    enabled = !state.isOffline && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BirthDateField(state: BaselineUiState, onClick: () -> Unit) {
    val dimens = HealthifyTheme.dimens
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(currentLocale())
    Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
        ReadOnlyClickableField(
            value = state.birthDate?.format(formatter).orEmpty(),
            label = stringResource(R.string.baseline_birth_date),
            onClick = onClick,
            trailingIcon = HealthifyIcons.Calendar,
            errorText = when {
                BaselineProblem.BIRTH_DATE_REQUIRED in state.problems -> stringResource(R.string.baseline_birth_date_required)
                BaselineProblem.BIRTH_DATE_IMPLAUSIBLE in state.problems -> stringResource(R.string.baseline_birth_date_range)
                else -> null
            },
        )
        state.ageYears?.let { age ->
            Text(
                text = stringResource(
                    R.string.baseline_age_auto,
                    pluralStringResource(R.plurals.baseline_age_years, age, age),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun FieldTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
internal fun ErrorLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

/** Unidad a la derecha del campo («cm», «kg», «%»), Label/Large en `onSurfaceVariant`. */
@Composable
internal fun UnitSuffix(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Selector de fecha (Material 3): solo fechas pasadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePicker(initial: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            HealthifyButton(
                text = stringResource(R.string.common_accept),
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
                style = HealthifyButtonStyle.Text,
            )
        },
        dismissButton = {
            HealthifyButton(text = stringResource(R.string.ds_cancel), onClick = onDismiss, style = HealthifyButtonStyle.Text)
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@Preview(name = "EV-1 · Datos base", widthDp = 360, heightDp = 800)
@Composable
private fun BaselinePreview() {
    HealthifyTheme {
        BaselineContent(
            state = BaselineUiState(
                birthDate = LocalDate.of(1995, 2, 15),
                ageYears = 31,
                sex = BiologicalSex.FEMALE,
                heightText = "168",
                conditions = setOf(MedicalCondition.HYPOTHYROIDISM),
            ),
            onBack = {},
            onBirthDateClick = {},
            onSexChange = {},
            onHeightChange = {},
            onConditionToggle = {},
            onSaveAndStart = {},
            onSaveAndExit = {},
        )
    }
}

@Preview(name = "EV-1 · Faltan datos", widthDp = 360, heightDp = 800)
@Composable
private fun BaselineErrorsPreview() {
    HealthifyTheme {
        BaselineContent(
            state = BaselineUiState(
                heightText = "30",
                problems = setOf(
                    BaselineProblem.BIRTH_DATE_REQUIRED,
                    BaselineProblem.SEX_REQUIRED,
                    BaselineProblem.HEIGHT_OUT_OF_RANGE,
                ),
            ),
            onBack = {},
            onBirthDateClick = {},
            onSexChange = {},
            onHeightChange = {},
            onConditionToggle = {},
            onSaveAndStart = {},
            onSaveAndExit = {},
        )
    }
}

@Preview(name = "EV-1 · Editar (cargando)", widthDp = 360, heightDp = 800)
@Composable
private fun BaselineEditingPreview() {
    HealthifyTheme {
        BaselineContent(
            state = BaselineUiState(editing = true, isLoading = true),
            onBack = {},
            onBirthDateClick = {},
            onSexChange = {},
            onHeightChange = {},
            onConditionToggle = {},
            onSaveAndStart = {},
            onSaveAndExit = {},
        )
    }
}
