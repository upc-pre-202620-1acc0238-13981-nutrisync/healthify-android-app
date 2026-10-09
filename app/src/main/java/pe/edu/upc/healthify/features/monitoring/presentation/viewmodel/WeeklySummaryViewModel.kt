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
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetLatestWeeklySummaryUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.presentation.state.WeeklySummaryUiState
import javax.inject.Inject

/** PT13.2 · el último resumen semanal con IA (IA-2). Al volver la conexión se vuelve a pedir. */
@HiltViewModel
class WeeklySummaryViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getLatestWeeklySummary: GetLatestWeeklySummaryUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklySummaryUiState())
    val state: StateFlow<WeeklySummaryUiState> = _state.asStateFlow()

    private var patientId: Long? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline && _state.value.summary == null) load()
                }
            }
            load()
        }
    }

    fun onRetry() = load()

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.summary == null, loadFailed = false) }
            val result = getLatestWeeklySummary(pid)
            _state.update { state ->
                when (val availability = result.getOrNull()) {
                    is WeeklySummaryAvailability.Ready ->
                        state.copy(isLoading = false, summary = availability.summary, notYet = false, isOff = false)
                    WeeklySummaryAvailability.NotYet -> state.copy(isLoading = false, summary = null, notYet = true, isOff = false)
                    WeeklySummaryAvailability.Off -> state.copy(isLoading = false, summary = null, notYet = false, isOff = true)
                    null -> state.copy(
                        isLoading = false,
                        loadFailed = true,
                        isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                    )
                }
            }
        }
    }
}
