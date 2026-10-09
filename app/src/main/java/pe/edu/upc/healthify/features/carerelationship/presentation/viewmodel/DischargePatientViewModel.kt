package pe.edu.upc.healthify.features.carerelationship.presentation.viewmodel

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
import pe.edu.upc.healthify.features.carerelationship.application.usecase.DischargePatientUseCase
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import pe.edu.upc.healthify.features.carerelationship.presentation.navigation.DischargePatientRoute
import pe.edu.upc.healthify.features.carerelationship.presentation.state.DischargeDialog
import pe.edu.upc.healthify.features.carerelationship.presentation.state.DischargePatientUiState
import javax.inject.Inject

sealed interface DischargePatientEvent {
    data class Discharged(val patientName: String) : DischargePatientEvent
}

/**
 * PR18 · Alta clínica (+ PR18.M confirmación). El motivo clínico es **obligatorio** (asimetría deliberada con PT23) y un
 * vínculo dado de alta nunca se reactiva, por eso se confirma antes de enviar.
 */
@HiltViewModel
class DischargePatientViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dischargePatient: DischargePatientUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val careLinkId: Long = checkNotNull(savedStateHandle[DischargePatientRoute.ARG_CARE_LINK_ID])
    private val patientName: String = savedStateHandle.get<String>(DischargePatientRoute.ARG_PATIENT_NAME).orEmpty()

    private val _state = MutableStateFlow(DischargePatientUiState())
    val state: StateFlow<DischargePatientUiState> = _state.asStateFlow()

    private val _events = Channel<DischargePatientEvent>(Channel.BUFFERED)
    val events: Flow<DischargePatientEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onReasonChange(value: String) = _state.update { it.copy(reason = value, reasonMissing = false) }

    /** «Dar de alta»: sin motivo no se abre la confirmación («Escribe el motivo clínico del alta»). */
    fun onDischargeRequested() {
        if (ClinicalReason.of(_state.value.reason) == null) {
            _state.update { it.copy(reasonMissing = true) }
            return
        }
        _state.update { it.copy(dialog = DischargeDialog.CONFIRM) }
    }

    /** PR18.M «Sí, dar de alta» (o «Reintentar» tras un fallo de red/servidor). */
    fun onConfirmed() {
        val reason = ClinicalReason.of(_state.value.reason)
        if (reason == null) {
            _state.update { it.copy(dialog = null, reasonMissing = true) }
            return
        }
        if (_state.value.isDischarging) return
        _state.update { it.copy(isDischarging = true) }
        viewModelScope.launch {
            val result = dischargePatient(careLinkId, reason)
            _state.update { it.copy(isDischarging = false) }
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> {
                    _state.update { it.copy(dialog = null) }
                    _events.send(DischargePatientEvent.Discharged(patientName))
                }
                error is DomainError.Validation && error.code == CLINICAL_REASON_REQUIRED ->
                    _state.update { it.copy(dialog = null, reasonMissing = true) }
                error is DomainError.Conflict -> _state.update { it.copy(dialog = DischargeDialog.ALREADY_DISCHARGED) }
                error is DomainError.Forbidden || error is DomainError.NotFound ->
                    _state.update { it.copy(dialog = DischargeDialog.NO_ACTIVE_LINK) }
                else -> _state.update { it.copy(dialog = DischargeDialog.SERVER_ERROR) }
            }
        }
    }

    /** «Este paciente ya había sido dado de alta.» → vuelve a PR1 igual que un alta. */
    fun onAlreadyDischargedConfirmed() {
        _state.update { it.copy(dialog = null) }
        viewModelScope.launch { _events.send(DischargePatientEvent.Discharged(patientName)) }
    }

    fun onDialogDismissed() = _state.update { it.copy(dialog = null) }

    private companion object {
        const val CLINICAL_REASON_REQUIRED = "ClinicalReasonRequired"
    }
}
