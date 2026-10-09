package pe.edu.upc.healthify.features.nutritionalcare.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPatientSummaryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.StartOrResumeConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientSummaryUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientTabEvent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * PAC-0 / PAC-1 / PAC-1.C · Resumen (RM-2). El botón fijo cambia según el estado: «Registrar datos base» (PAC-0),
 * «Iniciar consulta» (PAC-1) o «Continuar consulta» (PAC-1.C). Se relee al volver de la consulta o de EV-1.
 */
@HiltViewModel
class PatientSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPatientSummary: GetPatientSummaryUseCase,
    private val startOrResumeConsultation: StartOrResumeConsultationUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])

    private val _state = MutableStateFlow(PatientSummaryUiState())
    val state: StateFlow<PatientSummaryUiState> = _state.asStateFlow()

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
        if (_state.value.summary != null) load()
    }

    /** El botón fijo: PAC-0 → EV-1; PAC-1.C → el paso en curso; PAC-1 → inicia la consulta. */
    fun onPrimaryAction() {
        val summary = _state.value.summary ?: return
        val inProgress = summary.consultationInProgress
        when {
            !summary.hasBaseline -> _events.trySend(PatientTabEvent.OpenBaseline(editing = false))
            inProgress != null -> _events.trySend(PatientTabEvent.OpenConsultationStep(inProgress.step))
            else -> startConsultation()
        }
    }

    fun onEditBaseline() {
        _events.trySend(PatientTabEvent.OpenBaseline(editing = true))
    }

    fun onStartErrorDismissed() = _state.update { it.copy(showStartError = false) }

    fun onRetryStart() {
        _state.update { it.copy(showStartError = false) }
        startConsultation()
    }

    private fun startConsultation() {
        if (_state.value.isStartingConsultation) return
        val summary = _state.value.summary ?: return
        // DECISIÓN PAC-1: la consulta se asocia a la cita solo si es hoy (así la cita queda «hecha» al publicar).
        val today = LocalDate.now(clock)
        val followUpId = summary.nextFollowUp
            ?.takeIf { it.scheduledFor.atZone(clock.zone).toLocalDate() == today }
            ?.followUpId
        viewModelScope.launch {
            _state.update { it.copy(isStartingConsultation = true) }
            val result = startOrResumeConsultation(patientId, followUpId)
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

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.summary == null, loadFailed = false) }
            val result = getPatientSummary(patientId)
            _state.update { state ->
                result.getOrNull()?.let { state.copy(isLoading = false, summary = it) }
                    ?: state.copy(
                        isLoading = false,
                        loadFailed = state.summary == null,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
            }
        }
    }

    private companion object {
        const val BASELINE_REQUIRED = "BaselineRequired"
    }
}
