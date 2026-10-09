package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetConsultationsOverviewUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetSuggestedQuestionsUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.presentation.state.CheckInCard
import pe.edu.upc.healthify.features.monitoring.presentation.state.MyConsultationsUiState
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * PT25 · Mis consultas: compone MonitoringAdherence (consultas, check-in, preguntas sugeridas) y CareRelationship (si
 * el paciente tiene encendidas las preguntas sugeridas). Al volver a la pantalla (después de PT25.2) y al volver la
 * conexión se vuelve a pedir.
 */
@HiltViewModel
class MyConsultationsViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getConsultationsOverview: GetConsultationsOverviewUseCase,
    private val getSuggestedQuestions: GetSuggestedQuestionsUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MyConsultationsUiState())
    val state: StateFlow<MyConsultationsUiState> = _state.asStateFlow()

    private var patientId: Long? = null
    private var loadJob: Job? = null

    /**
     * Las preguntas de IA se piden una vez por consulta y por versión del check-in (el backend las regenera cuando el
     * check-in cambia; cada pedido cuenta para la cuota diaria).
     */
    private var suggestionsRequestedFor: String? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline && _state.value.loadFailed) load()
                }
            }
            load()
        }
    }

    fun onRetry() = load()

    /** Al volver de PT25.2 la respuesta pudo cambiar: se relee sin mostrar la carga. */
    fun onResume() {
        if (_state.value.hasLoaded) load()
    }

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = !it.hasLoaded, loadFailed = false) }
            val result = getConsultationsOverview(pid)
            val overview = result.getOrNull()
            if (overview == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = !it.hasLoaded,
                        isOffline = it.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
                }
                return@launch
            }
            val now = Instant.now(clock)
            _state.update {
                it.copy(
                    isLoading = false,
                    loadFailed = false,
                    hasLoaded = true,
                    next = overview.next,
                    checkInCard = overview.checkInCard(now),
                    past = overview.past,
                )
            }
            loadSuggestions(pid, overview)
        }
    }

    private suspend fun loadSuggestions(pid: Long, overview: ConsultationsOverview) {
        val followUpId = overview.next?.id?.value
        if (followUpId == null) {
            _state.update { it.copy(suggestedQuestions = emptyList()) }
            return
        }
        val requestKey = "$followUpId:${overview.checkIn?.let { it.editedAt ?: it.submittedAt }}"
        if (suggestionsRequestedFor == requestKey) return
        if (!getAiPreferences(pid).canSuggestQuestions) {
            _state.update { it.copy(suggestedQuestions = emptyList()) }
            return
        }
        val availability = getSuggestedQuestions(pid, followUpId).getOrNull() ?: return
        suggestionsRequestedFor = requestKey
        _state.update {
            it.copy(
                suggestedQuestions = (availability as? SuggestedQuestionsAvailability.Ready)?.suggestions?.questions
                    .orEmpty(),
            )
        }
    }
}

internal fun ConsultationsOverview.checkInCard(now: Instant): CheckInCard {
    val next = next ?: return CheckInCard.Hidden
    val sent = checkIn
    return when {
        sent != null -> CheckInCard.Sent(
            followUpId = next.id.value,
            submittedAt = sent.submittedAt,
            editedAt = sent.editedAt,
            canEdit = canEditCheckIn(now),
        )
        canAnswerCheckIn(now) -> CheckInCard.Answer(next.id.value)
        else -> CheckInCard.Hidden
    }
}
