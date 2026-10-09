package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetLinkTargetsReadStatusUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanHistoryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPractitionerRecordUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.StartOrResumeConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientPlanUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientTabEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PractitionerRecordUiState
import javax.inject.Inject

/** PAC-3 · Expediente (RM-4 con el token del profesional). Se relee al volver a la pestaña y al volver la conexión. */
@HiltViewModel
class PractitionerRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPractitionerRecord: GetPractitionerRecordUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])

    private val _state = MutableStateFlow(PractitionerRecordUiState())
    val state: StateFlow<PractitionerRecordUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.record != null) load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.record == null, loadFailed = false) }
            val result = getPractitionerRecord(patientId)
            _state.update { state ->
                result.getOrNull()?.let { state.copy(isLoading = false, record = it) }
                    ?: state.copy(
                        isLoading = false,
                        loadFailed = state.record == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
            }
        }
    }
}

/**
 * PAC-4 · Plan: el historial de versiones publicadas y si el paciente vio las vigentes (`targets-read-status`,
 * CR-3, en paralelo). «Ajustar plan» abre la consulta guiada (DECISIÓN PAC-4: PR11 «Ajustar» se reemplazó por EV-4, así
 * que se inicia o se reanuda la consulta).
 */
@HiltViewModel
class PatientPlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlanHistory: GetPlanHistoryUseCase,
    private val getLinkTargetsReadStatus: GetLinkTargetsReadStatusUseCase,
    private val startOrResumeConsultation: StartOrResumeConsultationUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    private val careLinkId: Long? = savedStateHandle.get<Long>(PatientArgs.CARE_LINK_ID)?.takeIf { it > 0 }

    private val _state = MutableStateFlow(PatientPlanUiState())
    val state: StateFlow<PatientPlanUiState> = _state.asStateFlow()

    private val _events = Channel<PatientTabEvent>(Channel.BUFFERED)
    val events: Flow<PatientTabEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline) load()
            }
        }
        load()
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.history != null) load()
    }

    fun onAdjustPlan() {
        if (_state.value.isStartingConsultation) return
        viewModelScope.launch {
            _state.update { it.copy(isStartingConsultation = true, showStartError = false) }
            val result = startOrResumeConsultation(patientId, scheduledFollowUpId = null)
            _state.update { it.copy(isStartingConsultation = false) }
            val consultation = result.getOrNull()
            when {
                consultation != null -> _events.send(PatientTabEvent.OpenConsultationStep(consultation.currentStep))
                result.domainErrorOrNull() == DomainError.Validation(BASELINE_REQUIRED) ->
                    _events.send(PatientTabEvent.OpenBaseline(editing = false))
                else -> _state.update { it.copy(showStartError = true) }
            }
        }
    }

    fun onStartErrorDismissed() = _state.update { it.copy(showStartError = false) }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.history == null, loadFailed = false) }
            val readStatus = careLinkId?.let { id -> async { getLinkTargetsReadStatus(id) } }
            val result = getPlanHistory(patientId)
            val status = readStatus?.await()?.getOrNull()
            _state.update { state ->
                result.getOrNull()?.let { state.copy(isLoading = false, history = it, readStatus = status) }
                    ?: state.copy(
                        isLoading = false,
                        loadFailed = state.history == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
            }
        }
    }

    private companion object {
        const val BASELINE_REQUIRED = "BaselineRequired"
    }
}
