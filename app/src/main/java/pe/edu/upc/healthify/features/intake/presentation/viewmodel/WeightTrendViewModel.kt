package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ConsumeSelfWeighInSavedNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetWeightTrendUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSelfWeighInSavedNoticeUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.ObserveSyncedSelfWeighInsUseCase
import pe.edu.upc.healthify.features.intake.presentation.state.WeeklyCard
import pe.edu.upc.healthify.features.intake.presentation.state.WeightTrendEvent
import pe.edu.upc.healthify.features.intake.presentation.state.WeightTrendUiState
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetLatestWeeklySummaryUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import javax.inject.Inject

/**
 * PT13 · Tendencia de peso, en la pestaña «Progreso». Compone Intake (tendencia, copia offline y aviso del último
 * autopesaje) y Monitoring (card «Tu semana», IA-2).
 *
 * DECISIÓN PT13: la card «Tu semana» aparece con resumen (titular de la IA) o sin él todavía (PT13.2.V); con la
 * función apagada (`403`/`503 AiFeatureDisabled`), sin conexión o con error, no aparece (el resumen no se guarda en el
 * teléfono).
 */
@HiltViewModel
class WeightTrendViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getWeightTrend: GetWeightTrendUseCase,
    private val getLatestWeeklySummary: GetLatestWeeklySummaryUseCase,
    private val observeSavedNotice: ObserveSelfWeighInSavedNoticeUseCase,
    private val consumeSavedNotice: ConsumeSelfWeighInSavedNoticeUseCase,
    private val observeSyncedSelfWeighIns: ObserveSyncedSelfWeighInsUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(WeightTrendUiState(weeks = GetWeightTrendUseCase.DEFAULT_WEEKS))
    val state: StateFlow<WeightTrendUiState> = _state.asStateFlow()

    private val _events = Channel<WeightTrendEvent>(Channel.BUFFERED)
    val events: Flow<WeightTrendEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null
    private var loadJob: Job? = null
    private var resumedOnce = false

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            launch {
                observeSavedNotice().filterNotNull().collect { outcome ->
                    consumeSavedNotice()
                    _events.send(WeightTrendEvent.ShowSaved(outcome))
                    load()
                }
            }
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) load()
                }
            }
            // Al volver la conexión la lectura suele llegar antes que el envío de la cola: se relee al aceptarse.
            launch { observeSyncedSelfWeighIns().collect { load() } }
            load()
        }
    }

    fun onRetry() = load()

    /** Al volver a la pestaña: un autopesaje sincronizado mientras tanto pudo mover la tendencia. */
    fun onResume() {
        if (!resumedOnce) {
            resumedOnce = true
            return
        }
        load()
    }

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hasContent = _state.value.trend != null || _state.value.hasNoTrend
            _state.update { it.copy(isLoading = !hasContent, loadFailed = false) }
            val weeklyCard = loadWeeklyCard(pid)
            val result = getWeightTrend(pid, _state.value.weeks)
            val lookup = result.getOrNull()
            _state.update { state ->
                when {
                    result.isSuccess -> state.copy(
                        isLoading = false,
                        trend = lookup?.takeIf { it.canBeDrawn },
                        // 404 WeightTrendNotFound o menos de 2 puntos en el rango: mismo copy de PT13.V.
                        hasNoTrend = lookup == null || !lookup.canBeDrawn,
                        weeklyCard = weeklyCard,
                    )
                    else -> state.copy(
                        isLoading = false,
                        loadFailed = true,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                        weeklyCard = weeklyCard,
                    )
                }
            }
        }
    }

    private suspend fun loadWeeklyCard(patientId: Long): WeeklyCard? =
        when (val availability = getLatestWeeklySummary(patientId).getOrNull()) {
            is WeeklySummaryAvailability.Ready -> WeeklyCard.Ready(availability.summary.headline)
            WeeklySummaryAvailability.NotYet -> WeeklyCard.NotYet
            WeeklySummaryAvailability.Off, null -> null
        }
}
