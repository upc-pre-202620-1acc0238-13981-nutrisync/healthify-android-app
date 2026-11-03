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
import pe.edu.upc.healthify.features.monitoring.application.usecase.CloseReferralUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetEvaluationDetailUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPlanHistoryUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetPractitionerRecordUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.StartOrResumeConsultationUseCase
import pe.edu.upc.healthify.features.nutritionalcare.presentation.navigation.PatientArgs
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.EvaluationSheetState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientPlanUiState
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PatientTabEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PractitionerRecordEvent
import pe.edu.upc.healthify.features.nutritionalcare.presentation.state.PractitionerRecordUiState
import javax.inject.Inject

/**
 * Record (with the practitioner's token). Reread on returning to the tab and when the connection returns. Tapping
 * an assessment requests its detail (`GET /nutritional-assessments/{id}`) and shows it in a sheet. Closing a
 * referral asks first and rereads the record, which brings the new status and closing date.
 */
@HiltViewModel
class PractitionerRecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPractitionerRecord: GetPractitionerRecordUseCase,
    private val getEvaluationDetail: GetEvaluationDetailUseCase,
    private val closeReferral: CloseReferralUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val patientId: Long = checkNotNull(savedStateHandle[PatientArgs.PATIENT_ID])

    private val _state = MutableStateFlow(PractitionerRecordUiState())
    val state: StateFlow<PractitionerRecordUiState> = _state.asStateFlow()

    private val _events = Channel<PractitionerRecordEvent>(Channel.BUFFERED)
    val events: Flow<PractitionerRecordEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var evaluationJob: Job? = null
    private var selectedAssessmentId: Long? = null

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

    fun onEvaluationSelected(assessmentId: Long) {
        selectedAssessmentId = assessmentId
        loadEvaluation(assessmentId)
    }

    fun onEvaluationRetry() {
        selectedAssessmentId?.let(::loadEvaluation)
    }

    fun onEvaluationDismissed() {
        evaluationJob?.cancel()
        selectedAssessmentId = null
        _state.update { it.copy(evaluationSheet = null) }
    }

    fun onCloseReferralRequested(referralId: Long) = _state.update { it.copy(referralToClose = referralId) }

    fun onCloseReferralDismissed() {
        if (!_state.value.isClosingReferral) _state.update { it.copy(referralToClose = null) }
    }

    fun onCloseReferralConfirmed() {
        val referralId = _state.value.referralToClose ?: return
        if (_state.value.isClosingReferral) return
        _state.update { it.copy(isClosingReferral = true) }
        viewModelScope.launch {
            val result = closeReferral(referralId)
            _state.update { it.copy(isClosingReferral = false, referralToClose = null) }
            val error = result.domainErrorOrNull()
            if (error == null) load() else _events.send(PractitionerRecordEvent.ReferralCloseFailed(error))
        }
    }

    private fun loadEvaluation(assessmentId: Long) {
        evaluationJob?.cancel()
        evaluationJob = viewModelScope.launch {
            _state.update { it.copy(evaluationSheet = EvaluationSheetState.Loading) }
            val detail = getEvaluationDetail(assessmentId).getOrNull()
            _state.update {
                it.copy(evaluationSheet = detail?.let(EvaluationSheetState::Loaded) ?: EvaluationSheetState.Failed)
            }
        }
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
 * Plan: the history of published versions and whether the patient saw the current ones (`targets-read-status`,
 * in parallel). "Adjust plan" opens the guided consultation (the old adjust screen was replaced by the targets step,
 * so it starts or resumes the consultation).
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
