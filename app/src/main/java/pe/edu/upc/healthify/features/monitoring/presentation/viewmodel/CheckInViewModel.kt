package pe.edu.upc.healthify.features.monitoring.presentation.viewmodel

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.application.usecase.GetAiPreferencesUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetCheckInUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetNextFollowUpUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetSuggestedQuestionsUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.SubmitCheckInUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInDifficulty
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.CheckInQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.OwnQuestion
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PlanFeeling
import pe.edu.upc.healthify.features.monitoring.presentation.navigation.CheckInRoute
import pe.edu.upc.healthify.features.monitoring.presentation.state.CheckInUiState
import pe.edu.upc.healthify.features.monitoring.presentation.state.OwnQuestionError
import pe.edu.upc.healthify.features.monitoring.presentation.state.SuggestionOption
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

sealed interface CheckInEvent {
    /** Enviado: vuelve a PT25, que relee y muestra PT25.3. */
    data object Submitted : CheckInEvent
}

/**
 * PT25.2 · Cuéntale cómo te fue (MA-4) con las sugerencias de IA (IA-4). Edita la respuesta guardada si existe y
 * bloquea el envío desde la hora de la consulta (también si el backend responde `409 CheckInLocked`).
 *
 * DECISIÓN PT25.2: el check-in es solo de la próxima consulta agendada; si la de la ruta ya no lo es (movida o
 * cancelada) se avisa y se vuelve. Sin conexión no se encola (no hay cola para el check-in): enviar sin red muestra
 * PT25.2.E y lo escrito se conserva.
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getNextFollowUp: GetNextFollowUpUseCase,
    private val getCheckIn: GetCheckInUseCase,
    private val submitCheckIn: SubmitCheckInUseCase,
    private val getSuggestedQuestions: GetSuggestedQuestionsUseCase,
    private val getAiPreferences: GetAiPreferencesUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val followUpId: Long = checkNotNull(savedStateHandle[CheckInRoute.ARG_FOLLOW_UP_ID]) {
        "CheckInRoute requires a followUpId"
    }

    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    private val _events = Channel<CheckInEvent>(Channel.BUFFERED)
    val events: Flow<CheckInEvent> = _events.receiveAsFlow()

    private var patientId: Long? = null
    private var followUp: NextFollowUp? = null

    /** Preguntas propias guardadas además de la primera (la pantalla edita una): se reenvían tal cual. */
    private var keptOwnQuestions: List<CheckInQuestion> = emptyList()
    private var loadJob: Job? = null

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

    fun onRetryLoad() = load()

    fun onFeelingSelected(feeling: PlanFeeling) {
        _state.update { it.copy(feeling = feeling, feelingMissing = false) }
    }

    fun onDifficultyToggled(difficulty: CheckInDifficulty, selected: Boolean) {
        _state.update { it.copy(difficulties = if (selected) it.difficulties + difficulty else it.difficulties - difficulty) }
    }

    fun onOwnQuestionChanged(text: String) {
        _state.update { it.copy(ownQuestion = text.take(CheckInUiState.MAX_QUESTION_LENGTH), ownQuestionError = null) }
    }

    fun onSuggestionToggled(text: String, selected: Boolean) {
        _state.update { state ->
            if (selected && !state.canSelectMoreSuggestions) return@update state
            state.copy(suggestions = state.suggestions.map { if (it.text == text) it.copy(selected = selected) else it })
        }
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.canSubmit) return
        val next = followUp ?: return
        if (!next.acceptsCheckInAt(Instant.now(clock))) {
            _state.update { it.copy(isLocked = true) }
            return
        }
        val feeling = current.feeling
        val own = CheckInQuestion.ownQuestion(current.ownQuestion)
        val ownError = when (own) {
            OwnQuestion.TooShort -> OwnQuestionError.TOO_SHORT
            OwnQuestion.TooLong -> OwnQuestionError.TOO_LONG
            else -> null
        }
        if (feeling == null || ownError != null) {
            _state.update { it.copy(feelingMissing = feeling == null, ownQuestionError = ownError) }
            return
        }
        val ownQuestions = listOfNotNull((own as? OwnQuestion.Valid)?.question)
            .plus(keptOwnQuestions)
            .distinct()
            .take(CheckInAnswer.MAX_QUESTIONS_PER_ORIGIN)
        val aiQuestions = current.suggestions
            .filter { it.selected }
            .mapNotNull { CheckInQuestion.aiSuggestion(it.text, it.aiGenerationId, it.language) }
            .distinct()
            .take(CheckInAnswer.MAX_QUESTIONS_PER_ORIGIN)
        val answer = CheckInAnswer(
            feeling = feeling,
            difficulties = current.difficulties,
            questions = (ownQuestions + aiQuestions).distinct(),
        )
        _state.update { it.copy(isSubmitting = true, submitFailed = false) }
        viewModelScope.launch {
            val result = submitCheckIn(followUpId, answer)
            if (result.isSuccess) {
                _state.update { it.copy(isSubmitting = false) }
                _events.send(CheckInEvent.Submitted)
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update { state ->
                when {
                    error is DomainError.Conflict && error.code == CODE_CHECK_IN_LOCKED ->
                        state.copy(isSubmitting = false, isLocked = true)
                    error is DomainError.Conflict || error is DomainError.NotFound ->
                        state.copy(isSubmitting = false, notScheduled = true)
                    else -> state.copy(
                        isSubmitting = false,
                        submitFailed = true,
                        isOffline = state.isOffline || error == DomainError.Network,
                    )
                }
            }
        }
    }

    /** PT25.2.E «Volver a intentarlo». */
    fun onRetrySubmit() {
        _state.update { it.copy(submitFailed = false) }
        onSubmit()
    }

    /** Atrás desde PT25.2.E: vuelve al formulario con lo escrito. */
    fun onDismissSubmitError() {
        _state.update { it.copy(submitFailed = false) }
    }

    private fun load() {
        val pid = patientId ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false) }
            val nextResult = getNextFollowUp(pid)
            if (nextResult.isFailure) {
                failLoad(nextResult.domainErrorOrNull())
                return@launch
            }
            val next = nextResult.getOrNull()
            if (next == null || next.id.value != followUpId) {
                _state.update { it.copy(isLoading = false, notScheduled = true) }
                return@launch
            }
            followUp = next
            val savedResult = getCheckIn(followUpId)
            if (savedResult.isFailure) {
                failLoad(savedResult.domainErrorOrNull())
                return@launch
            }
            val saved = savedResult.getOrNull()
            val locked = (saved?.isLocked == true) || !next.acceptsCheckInAt(Instant.now(clock))
            _state.update { it.prefilledWith(saved).copy(isLoading = false, isLocked = locked) }
            if (!locked) loadSuggestions(pid, saved)
        }
    }

    private fun failLoad(error: DomainError?) {
        _state.update {
            it.copy(isLoading = false, loadFailed = true, isOffline = it.isOffline || error == DomainError.Network)
        }
    }

    private fun CheckInUiState.prefilledWith(saved: PreVisitCheckIn?): CheckInUiState {
        if (saved == null) return this
        val own = saved.answer.ownQuestions
        keptOwnQuestions = own.drop(1)
        return copy(
            isEditing = true,
            feeling = saved.answer.feeling,
            difficulties = saved.answer.difficulties,
            ownQuestion = own.firstOrNull()?.text.orEmpty(),
            // Las sugerencias que ya había aceptado vuelven marcadas, con el idioma con que se guardaron.
            suggestions = saved.answer.aiQuestions.map {
                SuggestionOption(it.text, selected = true, aiGenerationId = it.aiGenerationId, language = it.language)
            },
        )
    }

    /** IA-4 solo si el paciente tiene encendidas las preguntas sugeridas; si falla, la card no aparece. */
    private suspend fun loadSuggestions(pid: Long, saved: PreVisitCheckIn?) {
        if (!getAiPreferences(pid).canSuggestQuestions) return
        val availability = getSuggestedQuestions(pid, followUpId).getOrNull()
        val ready = (availability as? SuggestedQuestionsAvailability.Ready)?.suggestions ?: return
        val accepted = saved?.answer?.aiQuestions?.map { it.text }.orEmpty().toSet()
        _state.update { state ->
            val fresh = ready.questions
                .filter { it.text !in accepted }
                .map { SuggestionOption(it.text, selected = false, aiGenerationId = ready.aiGenerationId, language = ready.language) }
            state.copy(suggestions = state.suggestions + fresh)
        }
    }

    private companion object {
        const val CODE_CHECK_IN_LOCKED = "CheckInLocked"
    }
}
