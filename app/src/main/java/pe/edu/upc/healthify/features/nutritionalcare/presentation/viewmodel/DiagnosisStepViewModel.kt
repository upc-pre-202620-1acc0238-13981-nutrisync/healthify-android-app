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
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetConsultationInProgressUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.IssueConsultationDiagnosisUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.SuggestDiagnosisUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.DiagnosisSuggestionUi
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.DiagnosisUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.SaveFailure
import javax.inject.Inject

/**
 * EV-3 · Paso 2 Diagnóstico (solo para el expediente profesional: el paciente no lo verá). Pide la sugerencia (IA-6, o
 * la regla del IMC si la IA no está) y ofrece la lista cerrada. «Usar sugerencia» guarda la generación de IA; «Elegir
 * otro» o tocar otro diagnóstico es una elección del profesional.
 *
 * DECISIÓN EV-3: si la sugerencia no llega (sin red, error) la tarjeta no se muestra y la lista sigue disponible.
 */
@HiltViewModel
class DiagnosisStepViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getConsultationInProgress: GetConsultationInProgressUseCase,
    private val suggestDiagnosis: SuggestDiagnosisUseCase,
    private val issueDiagnosis: IssueConsultationDiagnosisUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])
    val patientName: String = savedStateHandle.get<String>(PatientArgs.PATIENT_NAME).orEmpty()

    private val _state = MutableStateFlow(DiagnosisUiState())
    val state: StateFlow<DiagnosisUiState> = _state.asStateFlow()

    private val _events = Channel<ConsultationStepEvent>(Channel.BUFFERED)
    val events: Flow<ConsultationStepEvent> = _events.receiveAsFlow()

    private var consultation: Consultation? = null
    private var suggestion: DiagnosisSuggestion? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _state.update { it.copy(load = it.load.copy(isOffline = !online)) }
            }
        }
        load()
    }

    fun onRetry() = load()

    fun onUseSuggestion() {
        val current = suggestion ?: return
        _state.update { it.copy(selected = current.code, usesSuggestion = true, showRequired = false) }
    }

    /** «Elegir otro»: suelta la sugerencia para elegir de la lista. */
    fun onChooseOther() = _state.update { it.copy(selected = null, usesSuggestion = false) }

    fun onSelect(code: DiagnosisCode) =
        _state.update { it.copy(selected = code, usesSuggestion = false, showRequired = false) }

    fun onSaveFailureDismissed() = _state.update { it.copy(saveFailure = null) }

    fun onContinue() {
        val current = consultation ?: return
        val state = _state.value
        if (state.isSaving) return
        val code = state.selected ?: run {
            _state.update { it.copy(showRequired = true) }
            return
        }
        val choice = suggestion?.takeIf { state.usesSuggestion && it.code == code }
            ?.let(DiagnosisChoice::accepting)
            ?: DiagnosisChoice.Selected(code)
        // NC-7: el diagnóstico de esta consulta llega pendiente hasta publicar; ya está guardado. Repetir el paso
        // descartaría el pendiente y emitiría otro igual.
        val saved = current.diagnosis
        if (saved != null && saved.code == code && saved.source == choice.source) {
            _events.trySend(ConsultationStepEvent.Continue)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = issueDiagnosis(current.id, choice)
            _state.update { it.copy(isSaving = false) }
            val updated = result.getOrNull()
            val error = result.domainErrorOrNull()
            when {
                updated != null -> {
                    consultation = updated
                    _events.send(ConsultationStepEvent.Continue)
                }
                error.closesConsultation() -> _events.send(ConsultationStepEvent.ConsultationClosed)
                error == DomainError.Validation(STEP_OUT_OF_ORDER) -> _events.send(ConsultationStepEvent.Back)
                else -> _state.update { it.copy(saveFailure = SaveFailure(isNetwork = error == DomainError.Network)) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(load = it.load.copy(isLoading = true, loadFailed = false)) }
            val result = getConsultationInProgress(patientId)
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
            when {
                inProgress == null -> {
                    _events.send(ConsultationStepEvent.ConsultationClosed)
                    return@launch
                }
                inProgress.measurement == null -> {
                    _events.send(ConsultationStepEvent.Back)
                    return@launch
                }
            }
            consultation = inProgress
            val saved = inProgress.diagnosis
            _state.update {
                it.copy(
                    load = it.load.copy(isLoading = false, loadFailed = false),
                    selected = saved?.code ?: it.selected,
                )
            }
            if (suggestion == null) loadSuggestion(inProgress)
        }
    }

    private suspend fun loadSuggestion(inProgress: Consultation) {
        _state.update { it.copy(isSuggesting = true) }
        val result = suggestDiagnosis(inProgress.id)
        suggestion = result.getOrNull()
        val saved = inProgress.diagnosis
        // Al volver al paso: la sugerencia aceptada antes se muestra otra vez como usada.
        val acceptedBefore = suggestion?.let(DiagnosisChoice::accepting)
            ?.let { saved != null && saved.code == it.code && saved.source == it.source } == true
        _state.update {
            it.copy(
                isSuggesting = false,
                usesSuggestion = it.usesSuggestion || (acceptedBefore && it.selected == saved?.code),
                suggestion = suggestion?.let { s -> DiagnosisSuggestionUi(s.code, s.rationale, s.isFromAi, s.disclaimer) },
            )
        }
    }

    private companion object {
        const val STEP_OUT_OF_ORDER = "ConsultationStepOutOfOrder"
    }
}
