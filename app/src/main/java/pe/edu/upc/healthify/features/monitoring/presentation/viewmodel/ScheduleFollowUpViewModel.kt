package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetPatientRosterUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.RescheduleFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.SCHEDULED_FOR_MUST_BE_IN_FUTURE
import pe.edu.upc.healthify.features.monitoring.application.usecase.ScheduleFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpMoment
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.ScheduleFollowUpRoute
import pe.edu.upc.healthify.features.monitoring.presentation.state.PatientOption
import pe.edu.upc.healthify.features.monitoring.presentation.state.ScheduleFollowUpUiState
import pe.edu.upc.healthify.features.monitoring.presentation.state.ScheduleMomentError
import pe.edu.upc.healthify.features.monitoring.presentation.state.SchedulePatientError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

sealed interface ScheduleFollowUpEvent {
    data class Scheduled(val scheduledFor: Instant, val rescheduled: Boolean) : ScheduleFollowUpEvent
}

/**
 * PR17 · Agendar consulta (+ PR17.E, PR17.2) y reprogramar desde la Agenda. La fecha y hora deben ser futuras: se valida
 * en el teléfono con [FollowUpMoment] antes de enviar y el backend lo vuelve a validar (`400 ScheduledForMustBeInFuture`;
 * los backends anteriores a MA-5 respondían `404 ScheduledFollowUpNotFound`, que se lee igual).
 *
 * DECISIÓN PR17: la modalidad no aparece en el frame; se agenda presencial (default del backend). Sin paciente fijo, el
 * campo «Paciente» elige de la cartera (vínculos activos). Sin conexión o con un error del servidor → PR17.2.
 */
@HiltViewModel
class ScheduleFollowUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scheduleFollowUp: ScheduleFollowUpUseCase,
    private val rescheduleFollowUp: RescheduleFollowUpUseCase,
    private val getPatientRoster: GetPatientRosterUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState(savedStateHandle))
    val state: StateFlow<ScheduleFollowUpUiState> = _state.asStateFlow()

    private val _events = Channel<ScheduleFollowUpEvent>(Channel.BUFFERED)
    val events: Flow<ScheduleFollowUpEvent> = _events.receiveAsFlow()

    val today: LocalDate get() = LocalDate.now(clock)

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        if (!_state.value.patientFixed) loadPatients()
    }

    fun onPatientFieldClick() {
        if (_state.value.patientFixed) return
        _state.update { it.copy(showPatientPicker = true) }
        if (_state.value.patients == null && !_state.value.isLoadingPatients) loadPatients()
    }

    fun onPatientSelected(option: PatientOption) =
        _state.update { it.copy(patient = option, showPatientPicker = false, patientError = null) }

    fun onPatientPickerDismissed() = _state.update { it.copy(showPatientPicker = false) }

    fun onDateFieldClick() = _state.update { it.copy(showDatePicker = true) }

    fun onDateSelected(date: LocalDate) = _state.update { it.copy(date = date, showDatePicker = false, momentError = null) }

    fun onDatePickerDismissed() = _state.update { it.copy(showDatePicker = false) }

    fun onTimeFieldClick() = _state.update { it.copy(showTimePicker = true) }

    fun onTimeSelected(time: LocalTime) = _state.update { it.copy(time = time, showTimePicker = false, momentError = null) }

    fun onTimePickerDismissed() = _state.update { it.copy(showTimePicker = false) }

    fun onPreparationToggle(code: PreparationCode, selected: Boolean) = _state.update {
        it.copy(preparation = if (selected) it.preparation + code else it.preparation - code)
    }

    /** PR17.2 «Volver a intentarlo»: vuelve al formulario con lo escrito. */
    fun onBackToForm() = _state.update { it.copy(failed = false) }

    /** «Agendar». */
    fun onSubmit() {
        val current = _state.value
        if (current.isSaving) return
        val moment = FollowUpMoment.of(current.date, current.time, clock.zone, clock.instant())
        val patient = current.patient
        val momentError = when (moment) {
            FollowUpMoment.Incomplete -> ScheduleMomentError.MISSING
            FollowUpMoment.NotInFuture -> ScheduleMomentError.NOT_IN_FUTURE
            is FollowUpMoment.Valid -> null
        }
        val patientError = if (patient == null) SchedulePatientError.MISSING else null
        if (moment !is FollowUpMoment.Valid || patient == null) {
            _state.update { it.copy(momentError = momentError, patientError = patientError) }
            return
        }
        _state.update { it.copy(isSaving = true, momentError = null, patientError = null) }
        viewModelScope.launch {
            val preparation = FollowUpPreparation(current.preparation)
            val rescheduleId = current.rescheduleId
            val result: Result<AgendaVisit> = if (rescheduleId != null) {
                rescheduleFollowUp(rescheduleId, moment.instant, preparation)
            } else {
                scheduleFollowUp(NewFollowUp(PatientId(patient.patientId), moment.instant, preparation))
            }
            _state.update { it.copy(isSaving = false) }
            result.onSuccess { _events.send(ScheduleFollowUpEvent.Scheduled(it.scheduledFor, rescheduleId != null)) }
            result.domainErrorOrNull()?.let { onFailed(it, rescheduling = rescheduleId != null) }
        }
    }

    private fun onFailed(error: DomainError, rescheduling: Boolean) {
        _state.update {
            when {
                error is DomainError.Validation && error.code == SCHEDULED_FOR_MUST_BE_IN_FUTURE ->
                    it.copy(momentError = ScheduleMomentError.NOT_IN_FUTURE)
                // Backend anterior a MA-5: una fecha pasada era 404 ScheduledFollowUpNotFound (nunca se muestra el 404).
                !rescheduling && error is DomainError.NotFound && error.code == SCHEDULED_FOLLOW_UP_NOT_FOUND ->
                    it.copy(momentError = ScheduleMomentError.NOT_IN_FUTURE)
                error is DomainError.Conflict && error.code == PATIENT_ALREADY_HAS_FOLLOW_UP ->
                    it.copy(patientError = SchedulePatientError.ALREADY_SCHEDULED)
                error is DomainError.Forbidden -> it.copy(patientError = SchedulePatientError.NO_ACTIVE_LINK)
                else -> it.copy(failed = true)
            }
        }
    }

    private fun loadPatients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPatients = true) }
            val practitionerId = observeCurrentUser().first()?.id?.value
            val roster = practitionerId?.let { getPatientRoster(it).getOrNull() }
            _state.update { state ->
                state.copy(
                    isLoadingPatients = false,
                    patients = roster?.patients
                        ?.filter { it.isLinkActive }
                        ?.map { PatientOption(it.patientId, it.fullName) }
                        ?: state.patients,
                )
            }
        }
    }

    private fun initialState(handle: SavedStateHandle): ScheduleFollowUpUiState {
        val patientId = handle.get<Long>(ScheduleFollowUpRoute.ARG_PATIENT_ID) ?: 0L
        val patientName = handle.get<String>(ScheduleFollowUpRoute.ARG_PATIENT_NAME).orEmpty()
        val followUpId = handle.get<Long>(ScheduleFollowUpRoute.ARG_FOLLOW_UP_ID) ?: 0L
        val scheduledFor = handle.get<Long>(ScheduleFollowUpRoute.ARG_SCHEDULED_FOR) ?: 0L
        val preparation = handle.get<String>(ScheduleFollowUpRoute.ARG_PREPARATION).orEmpty()
            .split(',')
            .mapNotNull(PreparationCode::fromCode)
            .toSet()
        val fixed = patientId > 0
        // Al reprogramar, la fecha y la hora anteriores se dejan como punto de partida (en la zona del teléfono).
        val previous = scheduledFor.takeIf { followUpId > 0 && it > 0 }
            ?.let { Instant.ofEpochSecond(it).atZone(clock.zone) }
        return ScheduleFollowUpUiState(
            patient = if (fixed) PatientOption(patientId, patientName) else null,
            patientFixed = fixed,
            date = previous?.toLocalDate(),
            time = previous?.toLocalTime()?.withSecond(0)?.withNano(0),
            preparation = preparation,
            rescheduleId = followUpId.takeIf { it > 0 },
        )
    }

    private companion object {
        const val SCHEDULED_FOLLOW_UP_NOT_FOUND = "ScheduledFollowUpNotFound"
        const val PATIENT_ALREADY_HAS_FOLLOW_UP = "PatientAlreadyHasActiveScheduledFollowUp"
    }
}
