package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import pe.edu.upc.healthify.features.intake.application.usecase.GetMealIdeasUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogMealIdeaUseCase
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasEvent
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasFailure
import pe.edu.upc.healthify.features.intake.presentation.state.MealIdeasUiState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * PT14.4 Ideas para hoy → PT14.5 Detalle → «Registrar esta comida» (IA-3 + IN-6). Las ideas solo existen con
 * conexión (PT14.4.O); registrarlas sin conexión deja cada alimento en la cola.
 */
@HiltViewModel
class MealIdeasViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getMealIdeas: GetMealIdeasUseCase,
    private val logMealIdea: LogMealIdeaUseCase,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(MealIdeasUiState())
    val state: StateFlow<MealIdeasUiState> = _state.asStateFlow()

    private val _events = Channel<MealIdeasEvent>(Channel.BUFFERED)
    val events: Flow<MealIdeasEvent> = _events.receiveAsFlow()

    /** Las ideas ya vistas: «Ver otras ideas» las excluye. */
    private val seenIdeaIds = linkedSetOf<String>()

    /** Un `clientEntryId` por ingrediente y por idea: reintentar la misma idea nunca la duplica. */
    private val clientEntryIds = mutableMapOf<String, List<ClientEntryId>>()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                val wasOffline = _state.value.isOffline
                _state.update { it.copy(isOffline = !online) }
                if (online && wasOffline && _state.value.ideas == null) load(more = false)
            }
        }
        viewModelScope.launch { load(more = false) }
    }

    fun onRetry() {
        viewModelScope.launch { load(more = false) }
    }

    /** «Ver otras ideas». */
    fun onMoreIdeas() {
        viewModelScope.launch { load(more = true) }
    }

    fun onIdeaSelected(ideaId: String) = _state.update { it.copy(selectedIdeaId = ideaId) }

    /** Atrás desde el detalle vuelve a la lista. `false` si ya está en la lista (la pantalla se cierra). */
    fun onBack(): Boolean {
        if (_state.value.selectedIdeaId == null) return false
        _state.update { it.copy(selectedIdeaId = null) }
        return true
    }

    fun onDismissErrors() = _state.update { it.copy(showRegisterError = false, showMoreError = false) }

    private suspend fun load(more: Boolean) {
        val user = observeCurrentUser().first() ?: return
        _state.update {
            if (more) it.copy(isLoadingMore = true, showMoreError = false) else it.copy(isLoading = true, failure = null)
        }
        val result = getMealIdeas(user.id.value, LocalDate.now(clock), if (more) seenIdeaIds.toList() else emptyList())
        val ideas = result.getOrNull()
        if (ideas != null) {
            seenIdeaIds += ideas.ideas.map { it.id }
            _state.update { it.copy(isLoading = false, isLoadingMore = false, ideas = ideas, failure = null) }
            return
        }
        val error = result.domainErrorOrNull()
        _state.update {
            when {
                more -> it.copy(isLoadingMore = false, showMoreError = true, isOffline = it.isOffline || error == DomainError.Network)
                error == DomainError.Network -> it.copy(isLoading = false, isOffline = true)
                else -> it.copy(isLoading = false, failure = error.toFailure())
            }
        }
    }

    /** PT14.5 «Registrar esta comida». */
    fun onRegisterIdea() {
        val current = _state.value
        val idea = current.selectedIdea ?: return
        if (current.isRegistering || !idea.canBeLogged) return
        _state.update { it.copy(isRegistering = true, showRegisterError = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _state.update { it.copy(isRegistering = false) }
                return@launch
            }
            val ids = clientEntryIds.getOrPut(idea.id) { idea.loggableIngredients.map { ClientEntryId.random() } }
            val now = Instant.now(clock)
            val timestamp = LocalTimestamp.forNewEntry(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES), now)
            val result = logMealIdea(user.id.value, idea, timestamp, ids)
            val logged = result.getOrNull()
            _state.update { it.copy(isRegistering = false, showRegisterError = logged == null) }
            if (logged != null) _events.send(MealIdeasEvent.Logged(logged.outcome))
        }
    }

    private fun DomainError?.toFailure(): MealIdeasFailure = when (this) {
        is DomainError.Validation -> if (code == CODE_NOT_ENOUGH) MealIdeasFailure.NOT_ENOUGH_REMAINING else MealIdeasFailure.GENERIC
        is DomainError.NotFound -> MealIdeasFailure.NO_TARGETS
        is DomainError.Forbidden -> MealIdeasFailure.UNAVAILABLE
        is DomainError.Unexpected -> when (code) {
            CODE_FEATURE_DISABLED -> MealIdeasFailure.UNAVAILABLE
            CODE_RATE_LIMITED, HTTP_429 -> MealIdeasFailure.RATE_LIMITED
            else -> MealIdeasFailure.GENERIC
        }
        else -> MealIdeasFailure.GENERIC
    }

    private companion object {
        const val CODE_NOT_ENOUGH = "NotEnoughRemaining"
        const val CODE_FEATURE_DISABLED = "AiFeatureDisabled"
        const val CODE_RATE_LIMITED = "AiRateLimited"
        const val HTTP_429 = "HTTP_429"
    }
}
