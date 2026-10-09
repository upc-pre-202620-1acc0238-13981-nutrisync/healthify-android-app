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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.application.usecase.RecordReferralUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralField
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralSpecialty
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralValidation
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.RecordReferralRoute
import pe.edu.upc.healthify.features.monitoring.presentation.state.RecordReferralUiState
import javax.inject.Inject

sealed interface RecordReferralEvent {
    data class Recorded(val specialty: String) : RecordReferralEvent
}

/**
 * PR16 · Registrar derivación (+ PR16.E campos obligatorios, PR16.2 error). Una derivación registra que algo se decidió
 * en una fecha; no tiene flujo. Al registrar vuelve a la ficha con PAC-1-S3.
 */
@HiltViewModel
class RecordReferralViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordReferral: RecordReferralUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[RecordReferralRoute.ARG_PATIENT_ID])

    private val _state = MutableStateFlow(RecordReferralUiState())
    val state: StateFlow<RecordReferralUiState> = _state.asStateFlow()

    private val _events = Channel<RecordReferralEvent>(Channel.BUFFERED)
    val events: Flow<RecordReferralEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
    }

    fun onSpecialtyChange(value: String) = _state.update {
        it.copy(specialty = value.take(ReferralSpecialty.MAX_LENGTH), missing = it.missing - ReferralField.SPECIALTY)
    }

    fun onReasonChange(value: String) = _state.update {
        it.copy(reason = value.take(ReferralReason.MAX_LENGTH), missing = it.missing - ReferralField.REASON)
    }

    /** PR16.2 «Volver a intentarlo»: vuelve al formulario con lo escrito. */
    fun onBackToForm() = _state.update { it.copy(failed = false) }

    fun onNoActiveLinkDismissed() = _state.update { it.copy(noActiveLink = false) }

    /** «Registrar». */
    fun onSubmit() {
        val current = _state.value
        if (current.isSaving) return
        val referral = when (val validation = ReferralValidation.of(PatientId(patientId), current.specialty, current.reason)) {
            is ReferralValidation.Invalid -> {
                _state.update { it.copy(missing = validation.missing) }
                return
            }
            is ReferralValidation.Valid -> validation.referral
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = recordReferral(referral)
            _state.update { it.copy(isSaving = false) }
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> _events.send(RecordReferralEvent.Recorded(referral.specialty.text))
                error is DomainError.Validation && error.code == SPECIALTY_AND_REASON_REQUIRED ->
                    _state.update { it.copy(missing = setOf(ReferralField.SPECIALTY, ReferralField.REASON)) }
                error is DomainError.Forbidden -> _state.update { it.copy(noActiveLink = true) }
                else -> _state.update { it.copy(failed = true) }
            }
        }
    }

    private companion object {
        const val SPECIALTY_AND_REASON_REQUIRED = "SpecialtyAndReasonRequired"
    }
}
