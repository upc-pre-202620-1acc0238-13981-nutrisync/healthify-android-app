package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.DiscardConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetConsultationInProgressUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.RecordConsultationMeasurementUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.FieldProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementForm
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validate
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BodyMassIndex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ClinicalRange
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.NumberInput
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.MeasurementUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.SaveFailure
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.StepLoadState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

/** What the consultation steps ask of their screen. */
sealed interface ConsultationStepEvent {
    /** Step saved: on to the next. */
    data object Continue : ConsultationStepEvent

    /** "Exit and continue later". */
    data object Exit : ConsultationStepEvent

    /** A previous step is missing (e.g. the measurement): go back to it. */
    data object Back : ConsultationStepEvent

    /** No consultation in progress anymore (published or closed): back to the profile. */
    data object ConsultationClosed : ConsultationStepEvent

    /** Plan published: to the profile with "Plan published". */
    data object Published : ConsultationStepEvent
}

/** `409 ConsultationNotInProgress` or `404 ConsultationNotFound`: the consultation no longer accepts steps. */
internal fun DomainError?.closesConsultation(): Boolean =
    this == DomainError.Conflict("ConsultationNotInProgress") || this == DomainError.NotFound("ConsultationNotFound")

/** Saved number → field text ("74.2", "88"), the way the practitioner would type it. */
internal val formFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.ROOT))

/**
 * Step 1, today's measurement. Rehydrates what the in-progress consultation saved (resume) and reads the baseline
 * for the "Baseline" card and the live BMI (height is not asked again). If the input matches what is already saved,
 * "Continue to diagnosis" does not resend it: repeating step 1 would force redoing the diagnosis.
 */
@HiltViewModel
class MeasurementStepViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConsultationInProgress: GetConsultationInProgressUseCase,
    private val getPatientBaseline: GetPatientBaselineUseCase,
    private val recordMeasurement: RecordConsultationMeasurementUseCase,
    private val discardConsultation: DiscardConsultationUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    val patientName: String = savedStateHandle.get<String>(PatientArgs.PATIENT_NAME).orEmpty()

    private val _state = MutableStateFlow(MeasurementUiState())
    val state: StateFlow<MeasurementUiState> = _state.asStateFlow()

    private val _events = Channel<ConsultationStepEvent>(Channel.BUFFERED)
    val events: Flow<ConsultationStepEvent> = _events.receiveAsFlow()

    private var consultation: Consultation? = null
    private var heightCm: Double? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _state.update { it.copy(load = it.load.copy(isOffline = !online)) }
            }
        }
        load()
    }

    fun onRetry() = load()

    /** After editing the baseline it is reread (height changes the BMI). */
    fun onResume() {
        if (consultation == null) return
        viewModelScope.launch {
            getPatientBaseline(patientId).getOrNull()?.let { applyBaseline(it) }
        }
    }

    fun onWeightChange(text: String) = updateForm(MeasurementField.WEIGHT) { it.copy(weight = text) }
    fun onWaistChange(text: String) = updateForm(MeasurementField.WAIST) { it.copy(waist = text) }
    fun onBodyFatChange(text: String) = updateForm(MeasurementField.BODY_FAT) { it.copy(bodyFat = text) }
    fun onMealsPerDayChange(text: String) = updateForm(MeasurementField.MEALS_PER_DAY) { it.copy(mealsPerDay = text) }
    fun onWaterChange(text: String) = updateForm(MeasurementField.WATER) { it.copy(waterLiters = text) }
    fun onMealsOutChange(text: String) = updateForm(MeasurementField.MEALS_OUT) { it.copy(mealsOut = text) }
    fun onGlucoseChange(text: String) = updateForm(MeasurementField.GLUCOSE) { it.copy(glucose = text) }
    fun onCholesterolChange(text: String) = updateForm(MeasurementField.CHOLESTEROL) { it.copy(cholesterol = text) }
    fun onTriglyceridesChange(text: String) =
        updateForm(MeasurementField.TRIGLYCERIDES) { it.copy(triglycerides = text) }

    fun onProtocolToggle(check: ProtocolCheck) = updateForm(MeasurementField.PROTOCOL) {
        it.copy(protocolChecks = if (check in it.protocolChecks) it.protocolChecks - check else it.protocolChecks + check)
    }

    fun onActivityChange(level: ActivityLevel) = updateForm(MeasurementField.ACTIVITY) { it.copy(activityLevel = level) }

    fun onToggleHabits() = _state.update { it.copy(habitsExpanded = !it.habitsExpanded) }

    fun onToggleBiochemistry() = _state.update { it.copy(biochemistryExpanded = !it.biochemistryExpanded) }

    /** "‹" on step 1 opens the exit dialog. */
    fun onBack() = _state.update { it.copy(showExitDialog = true) }

    fun onStay() = _state.update { it.copy(showExitDialog = false) }

    /** "Exit and continue later": what is saved stays saved (each step persists on continue). */
    fun onExit() {
        _state.update { it.copy(showExitDialog = false) }
        _events.trySend(ConsultationStepEvent.Exit)
    }

    fun onSaveFailureDismissed() = _state.update { it.copy(saveFailure = null) }

    /** "Delete consultation" trash can (opened by mistake): asks for confirmation first. */
    fun onDiscard() {
        if (consultation == null || _state.value.isDiscarding) return
        _state.update { it.copy(showDiscardDialog = true) }
    }

    fun onDiscardDismissed() = _state.update { it.copy(showDiscardDialog = false) }

    fun onDiscardFailureDismissed() = _state.update { it.copy(discardFailure = null) }

    /** Confirmed: the consultation is discarded and it returns to the patient profile. */
    fun onDiscardConfirmed() {
        val current = consultation ?: return
        if (_state.value.isDiscarding) return
        viewModelScope.launch {
            _state.update { it.copy(showDiscardDialog = false, discardFailure = null, isDiscarding = true) }
            val result = discardConsultation(current.id)
            _state.update { it.copy(isDiscarding = false) }
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> _events.send(ConsultationStepEvent.Exit)
                // It was no longer in progress (another device closed it): nothing to delete.
                error.closesConsultation() -> _events.send(ConsultationStepEvent.ConsultationClosed)
                else -> _state.update { it.copy(discardFailure = SaveFailure(isNetwork = error == DomainError.Network)) }
            }
        }
    }

    fun onContinue() {
        val current = consultation ?: return
        if (_state.value.isSaving) return
        val measurement = when (val validation = _state.value.form.validate()) {
            is MeasurementValidation.Invalid -> {
                _state.update {
                    it.copy(
                        problems = validation.problems,
                        habitsExpanded = it.habitsExpanded || validation.problems.keys.any(::isHabitField),
                        biochemistryExpanded = it.biochemistryExpanded || validation.problems.keys.any(::isLabField),
                    )
                }
                return
            }
            is MeasurementValidation.Valid -> validation.measurement
        }
        val saved = current.measurement
        if (saved != null && measurement.sameAs(saved)) {
            _events.trySend(ConsultationStepEvent.Continue)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, problems = emptyMap()) }
            val result = recordMeasurement(current.id, measurement)
            _state.update { it.copy(isSaving = false) }
            val updated = result.getOrNull()
            val error = result.domainErrorOrNull()
            when {
                updated != null -> {
                    consultation = updated
                    _events.send(ConsultationStepEvent.Continue)
                }
                error.closesConsultation() -> _events.send(ConsultationStepEvent.ConsultationClosed)
                error == DomainError.Validation(IMPLAUSIBLE_MEASUREMENT) -> _state.update {
                    it.copy(problems = mapOf(MeasurementField.WEIGHT to FieldProblem.OUT_OF_RANGE))
                }
                else -> _state.update { it.copy(saveFailure = SaveFailure(isNetwork = error == DomainError.Network)) }
            }
        }
    }

    private fun updateForm(field: MeasurementField, change: (MeasurementForm) -> MeasurementForm) {
        _state.update {
            val form = change(it.form)
            it.copy(form = form, problems = it.problems - field, bmi = liveBmi(form.weight))
        }
    }

    /** BMI from the typed weight and the baseline height; `null` while the weight is invalid. */
    private fun liveBmi(weightText: String): Double? {
        val height = heightCm ?: return null
        val weight = (ClinicalRange.WEIGHT_KG.parse(weightText) as? NumberInput.Valid)?.value ?: return null
        return BodyMassIndex.of(weight, height).value
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(load = it.load.copy(isLoading = true, loadFailed = false)) }
            val baselineCall = async { getPatientBaseline(patientId) }
            val result = getConsultationInProgress(patientId)
            val baseline = baselineCall.await().getOrNull()
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        load = it.load.copy(
                            isLoading = false,
                            loadFailed = true,
                            isOffline = it.load.isOffline || result.domainErrorOrNull() == DomainError.Network,
                        ),
                    )
                }
                return@launch
            }
            val inProgress = result.getOrNull()
            if (inProgress == null) {
                _events.send(ConsultationStepEvent.ConsultationClosed)
                return@launch
            }
            consultation = inProgress
            baseline?.let { applyBaseline(it) }
            val saved = inProgress.measurement
            if (heightCm == null && saved != null) heightCm = saved.heightCm
            _state.update {
                val form = saved?.toForm() ?: it.form
                it.copy(
                    load = it.load.copy(isLoading = false, loadFailed = false),
                    checkIn = inProgress.patientCheckIn,
                    form = form,
                    bmi = liveBmi(form.weight),
                    habitsExpanded = saved?.habits != null,
                    biochemistryExpanded = saved?.biochemistry != null,
                )
            }
        }
    }

    private fun applyBaseline(baseline: PatientBaseline) {
        heightCm = baseline.heightCm
        _state.update {
            it.copy(
                baseline = BaselineSummary(baseline.sex, baseline.ageYears, baseline.heightCm, baseline.conditions),
                bmi = liveBmi(it.form.weight),
            )
        }
    }

    private fun ConsultationMeasurement.toForm() = MeasurementForm(
        weight = formFormat.format(weightKg),
        waist = waistCm?.let(formFormat::format).orEmpty(),
        bodyFat = bodyFatPercentage?.let(formFormat::format).orEmpty(),
        protocolChecks = protocolChecks,
        activityLevel = activityLevel,
        mealsPerDay = habits?.mealsPerDay?.toString().orEmpty(),
        waterLiters = habits?.waterLitersPerDay?.let(formFormat::format).orEmpty(),
        mealsOut = habits?.mealsOutPerWeek?.toString().orEmpty(),
        glucose = biochemistry?.glucoseMgDl?.let(formFormat::format).orEmpty(),
        cholesterol = biochemistry?.totalCholesterolMgDl?.let(formFormat::format).orEmpty(),
        triglycerides = biochemistry?.triglyceridesMgDl?.let(formFormat::format).orEmpty(),
    )

    private companion object {
        const val IMPLAUSIBLE_MEASUREMENT = "ImplausibleMeasurement"

        fun isHabitField(field: MeasurementField) =
            field == MeasurementField.MEALS_PER_DAY || field == MeasurementField.WATER || field == MeasurementField.MEALS_OUT

        fun isLabField(field: MeasurementField) =
            field == MeasurementField.GLUCOSE || field == MeasurementField.CHOLESTEROL ||
                field == MeasurementField.TRIGLYCERIDES
    }
}
