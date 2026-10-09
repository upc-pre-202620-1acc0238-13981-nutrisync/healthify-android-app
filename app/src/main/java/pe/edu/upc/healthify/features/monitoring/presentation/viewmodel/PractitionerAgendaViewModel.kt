package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.CancelFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetUpcomingFollowUpsUseCase
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaDialog
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaRow
import pe.edu.upc.healthify.features.monitoring.presentation.state.AgendaUiState
import javax.inject.Inject

sealed interface AgendaEvent {
    /** «Reprogramar» → PR17 con la consulta prellenada. */
    data class OpenReschedule(val row: AgendaRow) : AgendaEvent

    /** Snackbar «Consulta cancelada». */
    data object Cancelled : AgendaEvent
}

/**
 * PR17.0 · Agenda: las consultas agendadas desde ahora. Se relee al volver a la pestaña (PR17.0-S tras agendar) y al
 * volver la conexión.
 *
 * DECISIÓN PR17.0: el frame no dice qué hace tocar una consulta (tiene chevron); abre una hoja con «Reprogramar» y
 * «Cancelar consulta» (MA-5). Cancelar pide confirmación y no cierra el vínculo.
 */
@HiltViewModel
class PractitionerAgendaViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getUpcomingFollowUps: GetUpcomingFollowUpsUseCase,
    private val cancelFollowUp: CancelFollowUpUseCase,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(AgendaUiState())
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    private val _events = Channel<AgendaEvent>(Channel.BUFFERED)
    val events: Flow<AgendaEvent> = _events.receiveAsFlow()

    private var practitionerId: Long? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            practitionerId = user.id.value
            launch {
                connectivityObserver.isOnline.collect { online ->
                    val wasOffline = _state.value.isOffline
                    _state.update { it.copy(isOffline = !online) }
                    if (online && wasOffline) load()
                }
            }
            load()
        }
    }

    fun onRetry() = load()

    fun onResume() {
        if (_state.value.visits != null) load()
    }

    fun onVisitSelected(row: AgendaRow) = _state.update { it.copy(selected = row) }

    fun onSheetDismissed() = _state.update { it.copy(selected = null) }

    fun onReschedule() {
        val row = _state.value.selected ?: return
        _state.update { it.copy(selected = null) }
        viewModelScope.launch { _events.send(AgendaEvent.OpenReschedule(row)) }
    }

    fun onCancelRequested() = _state.update { it.copy(dialog = AgendaDialog.CONFIRM_CANCEL) }

    fun onDialogDismissed() = _state.update {
        it.copy(dialog = null, selected = if (it.dialog == AgendaDialog.CONFIRM_CANCEL) it.selected else null)
    }

    /** «Sí, cancelar» (o «Reintentar» tras un fallo de red/servidor). */
    fun onCancelConfirmed() {
        val row = _state.value.selected ?: return
        if (_state.value.isCancelling) return
        _state.update { it.copy(isCancelling = true) }
        viewModelScope.launch {
            val result = cancelFollowUp(row.followUpId)
            _state.update { it.copy(isCancelling = false) }
            val error = result.domainErrorOrNull()
            when {
                result.isSuccess -> {
                    _state.update { state ->
                        state.copy(
                            dialog = null,
                            selected = null,
                            visits = state.visits?.filterNot { it.followUpId == row.followUpId },
                        )
                    }
                    _events.send(AgendaEvent.Cancelled)
                    load()
                }
                error is DomainError.NotFound || error is DomainError.Conflict || error is DomainError.Forbidden -> {
                    _state.update { it.copy(dialog = AgendaDialog.NOT_CANCELLABLE) }
                    load()
                }
                else -> _state.update { it.copy(dialog = AgendaDialog.SERVER_ERROR) }
            }
        }
    }

    private fun load() {
        val id = practitionerId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.visits == null, loadFailed = false) }
            val result = getUpcomingFollowUps(id)
            _state.update { state ->
                result.getOrNull()?.let { visits ->
                    state.copy(isLoading = false, visits = visits.map(AgendaRow::of))
                } ?: state.copy(
                    isLoading = false,
                    loadFailed = state.visits == null,
                    isOffline = state.isOffline || result.domainErrorOrNull() == DomainError.Network,
                )
            }
        }
    }
}
