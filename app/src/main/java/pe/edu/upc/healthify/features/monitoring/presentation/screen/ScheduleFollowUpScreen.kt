package pe.edu.upc.healthify.features.monitoring.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.BottomSheetStaticFrame
import pe.edu.upc.healthify.core.designsystem.component.FailureState
import pe.edu.upc.healthify.core.designsystem.component.HealthifyBottomSheet
import pe.edu.upc.healthify.core.designsystem.component.HealthifyButton
import pe.edu.upc.healthify.core.designsystem.component.HealthifyFilterChip
import pe.edu.upc.healthify.core.designsystem.component.HealthifyTopAppBar
import pe.edu.upc.healthify.core.designsystem.component.ObserveEvents
import pe.edu.upc.healthify.core.designsystem.component.OfflineBanner
import pe.edu.upc.healthify.core.designsystem.component.OfflineBannerType
import pe.edu.upc.healthify.core.designsystem.component.ReadOnlyClickableField
import pe.edu.upc.healthify.core.designsystem.component.SectionCard
import pe.edu.upc.healthify.core.designsystem.component.SkeletonListItem
import pe.edu.upc.healthify.core.designsystem.component.SystemBarsAppearance
import pe.edu.upc.healthify.core.designsystem.component.currentLocale
import pe.edu.upc.healthify.core.designsystem.icon.HealthifyIcons
import pe.edu.upc.healthify.core.designsystem.theme.HealthifyTheme
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.presentation.components.FollowUpDatePickerDialog
import pe.edu.upc.healthify.features.monitoring.presentation.components.FollowUpTimePickerDialog
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientOption
import pe.edu.upc.healthify.features.monitoring.presentation.state.ScheduleFollowUpUiState
import pe.edu.upc.healthify.features.monitoring.presentation.state.ScheduleMomentError
import pe.edu.upc.healthify.features.monitoring.presentation.state.SchedulePatientError
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.ScheduleFollowUpEvent
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.ScheduleFollowUpViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class ScheduleFollowUpActions(
    val onBack: () -> Unit = {},
    val onPatientFieldClick: () -> Unit = {},
    val onPatientSelected: (PatientOption) -> Unit = {},
    val onPatientPickerDismissed: () -> Unit = {},
    val onDateFieldClick: () -> Unit = {},
    val onDateSelected: (LocalDate) -> Unit = {},
    val onDatePickerDismissed: () -> Unit = {},
    val onTimeFieldClick: () -> Unit = {},
    val onTimeSelected: (LocalTime) -> Unit = {},
    val onTimePickerDismissed: () -> Unit = {},
    val onPreparationToggle: (PreparationCode, Boolean) -> Unit = { _, _ -> },
    val onSubmit: () -> Unit = {},
    val onBackToForm: () -> Unit = {},
)

/** PR17 · Agendar consulta (+ PR17.E fecha no futura, PR17.2 no se pudo agendar) y reprogramar. */
@Composable
fun ScheduleFollowUpScreen(
    onBack: () -> Unit,
    onScheduled: (scheduledFor: Instant, rescheduled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleFollowUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveEvents(viewModel.events) { event ->
        when (event) {
            is ScheduleFollowUpEvent.Scheduled -> onScheduled(event.scheduledFor, event.rescheduled)
        }
    }
    ScheduleFollowUpContent(
        state = state,
        today = viewModel.today,
        actions = ScheduleFollowUpActions(
            onBack = onBack,
            onPatientFieldClick = viewModel::onPatientFieldClick,
            onPatientSelected = viewModel::onPatientSelected,
            onPatientPickerDismissed = viewModel::onPatientPickerDismissed,
            onDateFieldClick = viewModel::onDateFieldClick,
            onDateSelected = viewModel::onDateSelected,
            onDatePickerDismissed = viewModel::onDatePickerDismissed,
            onTimeFieldClick = viewModel::onTimeFieldClick,
            onTimeSelected = viewModel::onTimeSelected,
            onTimePickerDismissed = viewModel::onTimePickerDismissed,
            onPreparationToggle = viewModel::onPreparationToggle,
            onSubmit = viewModel::onSubmit,
            onBackToForm = viewModel::onBackToForm,
        ),
        modifier = modifier,
    )
}

@Composable
fun ScheduleFollowUpContent(
    state: ScheduleFollowUpUiState,
    today: LocalDate,
    actions: ScheduleFollowUpActions,
    modifier: Modifier = Modifier,
) {
    val dimens = HealthifyTheme.dimens
    SystemBarsAppearance(darkBackground = false)
    if (state.failed) {
        // PR17.2: lo escrito se conserva; «Volver a intentarlo» vuelve al formulario.
        FailureState(
            title = stringResource(R.string.schedule_failed_title),
            text = stringResource(R.string.schedule_failed_body),
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
            .navigationBarsPadding(),
    ) {
        HealthifyTopAppBar(
            title = stringResource(if (state.isRescheduling) R.string.schedule_reschedule_title else R.string.schedule_title),
            onBack = actions.onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimens.space16),
            verticalArrangement = Arrangement.spacedBy(dimens.space24),
        ) {
            if (state.isOffline) OfflineBanner(type = OfflineBannerType.RequiresNetwork)
            MomentCard(state, actions)
            PreparationCard(state, actions)
            HealthifyButton(
                text = stringResource(
                    when {
                        state.isSaving -> R.string.schedule_saving
                        state.isRescheduling -> R.string.schedule_reschedule_submit
                        else -> R.string.schedule_submit
                    },
                ),
                onClick = actions.onSubmit,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.showPatientPicker) {
        HealthifyBottomSheet(onDismissRequest = actions.onPatientPickerDismissed) {
            PatientPickerContent(state = state, onSelect = actions.onPatientSelected)
        }
    }
    if (state.showDatePicker) {
        FollowUpDatePickerDialog(
            initial = state.date,
            today = today,
            onConfirm = actions.onDateSelected,
            onDismiss = actions.onDatePickerDismissed,
        )
    }
    if (state.showTimePicker) {
        FollowUpTimePickerDialog(initial = state.time, onConfirm = actions.onTimeSelected, onDismiss = actions.onTimePickerDismissed)
    }
}

@Composable
private fun MomentCard(state: ScheduleFollowUpUiState, actions: ScheduleFollowUpActions) {
    val dimens = HealthifyTheme.dimens
    val locale = currentLocale()
    SectionCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = dimens.space16) {
        ReadOnlyClickableField(
            value = state.patient?.fullName.orEmpty(),
            label = stringResource(R.string.schedule_patient),
            onClick = actions.onPatientFieldClick,
            enabled = !state.isSaving && !state.patientFixed,
            errorText = when (state.patientError) {
                SchedulePatientError.MISSING -> stringResource(R.string.schedule_patient_missing)
                SchedulePatientError.ALREADY_SCHEDULED -> stringResource(R.string.schedule_patient_already_scheduled)
                SchedulePatientError.NO_ACTIVE_LINK -> stringResource(R.string.practitioner_no_active_link_body)
                null -> null
            },
            trailingIcon = if (state.patientFixed) null else HealthifyIcons.ChevronRight,
        )
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space8)) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.space12)) {
                val momentError = when (state.momentError) {
                    ScheduleMomentError.MISSING -> stringResource(R.string.schedule_moment_missing)
                    ScheduleMomentError.NOT_IN_FUTURE -> stringResource(R.string.schedule_moment_not_future)
                    null -> null
                }
                ReadOnlyClickableField(
                    value = state.date?.let { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(it) }
                        .orEmpty(),
                    label = stringResource(R.string.schedule_date),
                    onClick = actions.onDateFieldClick,
                    enabled = !state.isSaving,
                    errorText = momentError,
                    trailingIcon = HealthifyIcons.Calendar,
                    modifier = Modifier.weight(1f),
                )
                ReadOnlyClickableField(
                    value = state.time?.let { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(it) }
                        .orEmpty(),
                    label = stringResource(R.string.schedule_time),
                    onClick = actions.onTimeFieldClick,
                    enabled = !state.isSaving,
                    trailingIcon = HealthifyIcons.Clock,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.schedule_moment_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** «¿Cómo debe prepararse?» (MA-2): el paciente lo ve en PT25.1. */
@Composable
private fun PreparationCard(state: ScheduleFollowUpUiState, actions: ScheduleFollowUpActions) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.schedule_preparation_title), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
        Text(
            text = stringResource(R.string.schedule_preparation_help),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimens.space8),
            verticalArrangement = Arrangement.spacedBy(dimens.space8),
        ) {
            PreparationCode.entries.forEach { code ->
                HealthifyFilterChip(
                    text = stringResource(code.chipRes),
                    selected = code in state.preparation,
                    onSelectedChange = { actions.onPreparationToggle(code, it) },
                    enabled = !state.isSaving,
                )
            }
        }
    }
}

@Composable
private fun PatientPickerContent(state: ScheduleFollowUpUiState, onSelect: (PatientOption) -> Unit) {
    val dimens = HealthifyTheme.dimens
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = dimens.space24, end = dimens.space24, top = dimens.space16, bottom = dimens.space24),
        verticalArrangement = Arrangement.spacedBy(dimens.space8),
    ) {
        Text(
            text = stringResource(R.string.schedule_patient_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        val patients = state.patients
        when {
            patients == null && state.isLoadingPatients -> repeat(2) { SkeletonListItem(modifier = Modifier.fillMaxWidth()) }
            patients.isNullOrEmpty() -> Text(
                text = stringResource(R.string.schedule_patient_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            else -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                patients.forEachIndexed { index, option ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant, thickness = dimens.borderThin)
                    Text(
                        text = option.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = dimens.button)
                            .selectable(
                                selected = option.patientId == state.patient?.patientId,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = dimens.space12),
                    )
                }
            }
        }
    }
}

/** Texto corto del chip de preparación para el profesional («En ayunas»); el paciente ve la indicación completa. */
private val PreparationCode.chipRes: Int
    get() = when (this) {
        PreparationCode.FASTING -> R.string.schedule_preparation_fasting
        PreparationCode.LIGHT_CLOTHING -> R.string.schedule_preparation_light_clothing
        PreparationCode.BRING_BLOOD_TESTS -> R.string.schedule_preparation_blood_tests
        PreparationCode.EMPTY_BLADDER -> R.string.schedule_preparation_empty_bladder
    }

private val previewToday: LocalDate = LocalDate.of(2026, 9, 15)

private val previewState = ScheduleFollowUpUiState(
    patient = PatientOption(11, "Ana Flores"),
    preparation = setOf(PreparationCode.FASTING, PreparationCode.LIGHT_CLOTHING, PreparationCode.BRING_BLOOD_TESTS),
)

@Preview(name = "PR17 · Agendar consulta", widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleFollowUpPreview() {
    HealthifyTheme { ScheduleFollowUpContent(state = previewState, today = previewToday, actions = ScheduleFollowUpActions()) }
}

@Preview(name = "PR17.E · Fecha no futura", widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleFollowUpNotFuturePreview() {
    HealthifyTheme {
        ScheduleFollowUpContent(
            state = previewState.copy(date = LocalDate.of(2026, 9, 12), momentError = ScheduleMomentError.NOT_IN_FUTURE),
            today = previewToday,
            actions = ScheduleFollowUpActions(),
        )
    }
}

@Preview(name = "PR17.2 · No se pudo agendar", widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleFollowUpFailedPreview() {
    HealthifyTheme {
        ScheduleFollowUpContent(state = previewState.copy(failed = true), today = previewToday, actions = ScheduleFollowUpActions())
    }
}

@Preview(name = "PR17 · Sin conexión", widthDp = 360, heightDp = 800)
@Composable
private fun ScheduleFollowUpOfflinePreview() {
    HealthifyTheme {
        ScheduleFollowUpContent(state = previewState.copy(isOffline = true), today = previewToday, actions = ScheduleFollowUpActions())
    }
}

@Preview(name = "PR17 · Elegir paciente", widthDp = 360, heightDp = 360)
@Composable
private fun PatientPickerPreview() {
    HealthifyTheme {
        BottomSheetStaticFrame {
            PatientPickerContent(
                state = previewState.copy(patients = listOf(PatientOption(11, "Ana Flores"), PatientOption(12, "Carlos Quispe"))),
                onSelect = {},
            )
        }
    }
}
