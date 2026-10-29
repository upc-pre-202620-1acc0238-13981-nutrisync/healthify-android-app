package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SavePatientBaselineUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.StartOrResumeConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validateBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BirthDate
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.BaselineEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.BaselineUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.SaveFailure
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

/**
 * Baseline. The first time it is recorded (`POST`) and "Save and start consultation" goes straight to step 1; when
 * editing (from Summary or the measurement step) it is preloaded and saved with `PUT`. Age comes from the phone clock.
 */
@HiltViewModel
class BaselineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPatientBaseline: GetPatientBaselineUseCase,
    private val savePatientBaseline: SavePatientBaselineUseCase,
    private val startOrResumeConsultation: StartOrResumeConsultationUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    val patientName: String = savedStateHandle.get<String>(PatientArgs.PATIENT_NAME).orEmpty()
    private val editing: Boolean = savedStateHandle.get<Boolean>(PatientArgs.EDITING) ?: false

    private val _state = MutableStateFlow(BaselineUiState(editing = editing, isLoading = editing))
    val state: StateFlow<BaselineUiState> = _state.asStateFlow()

    private val _events = Channel<BaselineEvent>(Channel.BUFFERED)
    val events: Flow<BaselineEvent> = _events.receiveAsFlow()

    /** If it is already recorded (editing, or an earlier `POST` that succeeded), it is saved with `PUT`. */
    private var alreadyRecorded = editing

    /** The last thing saved: a retry of "start consultation" does not resend it. */
    private var saved: NewBaseline? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        if (editing) prefill()
    }

    fun onBirthDateClick() = _state.update { it.copy(showDatePicker = true) }

    fun onDatePickerDismissed() = _state.update { it.copy(showDatePicker = false) }

    fun onBirthDateChange(date: LocalDate) {
        val today = LocalDate.now(clock)
        _state.update {
            it.copy(
                birthDate = date,
                showDatePicker = false,
                ageYears = date.takeIf { d -> BirthDate.isPlausible(d, today) }?.let { d -> BirthDate.ageOn(d, today) },
                problems = it.problems - BaselineProblem.BIRTH_DATE_REQUIRED - BaselineProblem.BIRTH_DATE_IMPLAUSIBLE,
            )
        }
    }

    fun onSexChange(sex: BiologicalSex) =
        _state.update { it.copy(sex = sex, problems = it.problems - BaselineProblem.SEX_REQUIRED) }

    fun onHeightChange(text: String) = _state.update {
        it.copy(
            heightText = text,
            problems = it.problems - BaselineProblem.HEIGHT_REQUIRED - BaselineProblem.HEIGHT_OUT_OF_RANGE,
        )
    }

    fun onConditionToggle(condition: MedicalCondition) = _state.update {
        it.copy(conditions = if (condition in it.conditions) it.conditions - condition else it.conditions + condition)
    }

    /** "Save and start consultation" (first time only). */
    fun onSaveAndStart() = save(startConsultation = true)

    /** "Save and exit". */
    fun onSaveAndExit() = save(startConsultation = false)

    fun onSaveFailureDismissed() = _state.update { it.copy(saveFailure = null) }

    fun onRetrySave() {
        _state.update { it.copy(saveFailure = null) }
        save(startConsultation = !editing)
    }

    private fun save(startConsultation: Boolean) {
        val current = _state.value
        if (current.isSaving) return
        val validation = validateBaseline(
            current.birthDate,
            current.sex,
            current.heightText,
            current.conditions,
            LocalDate.now(clock),
        )
        val baseline = when (validation) {
            is BaselineValidation.Invalid -> {
                _state.update { it.copy(problems = validation.problems) }
                return
            }
            is BaselineValidation.Valid -> validation.baseline
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, problems = emptySet()) }
            if (saved != baseline) {
                val result = savePatientBaseline(patientId, baseline, alreadyRecorded)
                if (result.isFailure) {
                    _state.update { it.copy(isSaving = false).withSaveError(result.domainErrorOrNull()) }
                    return@launch
                }
                alreadyRecorded = true
                saved = baseline
            }
            if (!startConsultation) {
                _state.update { it.copy(isSaving = false) }
                _events.send(BaselineEvent.Saved)
                return@launch
            }
            val consultation = startOrResumeConsultation(patientId, scheduledFollowUpId = null)
            _state.update { it.copy(isSaving = false) }
            consultation.getOrNull()?.let { _events.send(BaselineEvent.ConsultationStarted(it.currentStep)) }
                ?: _state.update {
                    it.copy(saveFailure = SaveFailure(consultation.domainErrorOrNull() == DomainError.Network))
                }
        }
    }

    /** `400 InvalidBirthDate`/`InvalidHeight` flag their field; anything else is a notice with "Retry". */
    private fun BaselineUiState.withSaveError(error: DomainError?): BaselineUiState = when (error) {
        DomainError.Validation(INVALID_BIRTH_DATE) -> copy(problems = setOf(BaselineProblem.BIRTH_DATE_IMPLAUSIBLE))
        DomainError.Validation(INVALID_HEIGHT) -> copy(problems = setOf(BaselineProblem.HEIGHT_OUT_OF_RANGE))
        else -> copy(saveFailure = SaveFailure(isNetwork = error == DomainError.Network))
    }

    private fun prefill() {
        viewModelScope.launch {
            val baseline = getPatientBaseline(patientId).getOrNull()
            if (baseline == null) {
                // With no saved baseline (or unable to read it), it is recorded as the first time.
                alreadyRecorded = false
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    birthDate = baseline.birthDate,
                    sex = baseline.sex,
                    heightText = heightFormat.format(baseline.heightCm),
                    conditions = baseline.conditions,
                    ageYears = baseline.ageYears,
                )
            }
        }
    }

    private companion object {
        const val INVALID_BIRTH_DATE = "InvalidBirthDate"
        const val INVALID_HEIGHT = "InvalidHeight"

        /** The preloaded height is written the way the practitioner would ("168" or "168.5"). */
        val heightFormat = DecimalFormat("0.#", DecimalFormatSymbols(Locale.ROOT))
    }
}
