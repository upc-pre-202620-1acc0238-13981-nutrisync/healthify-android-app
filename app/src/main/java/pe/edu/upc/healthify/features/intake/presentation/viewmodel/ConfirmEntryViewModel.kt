package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import pe.edu.upc.healthify.features.intake.application.usecase.ConfirmDiaryEntryUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.GetDiaryDayUseCase
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.presentation.navigation.ConfirmEntryRoute
import pe.edu.upc.healthify.features.intake.presentation.navigation.PickedFoodResult
import pe.edu.upc.healthify.features.intake.presentation.state.ConfirmEntryEvent
import pe.edu.upc.healthify.features.intake.presentation.state.ConfirmEntryUiState
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

/**
 * PT14 «Confirmar porción» → PT8 de una entrada «Por confirmar» (F16): confirma la propuesta o la ajusta (otro plato
 * u otros gramos). El momento ya declarado no se toca (*Declared Local Timestamp Never Rewritten*).
 */
@HiltViewModel
class ConfirmEntryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getDiaryDay: GetDiaryDayUseCase,
    private val confirmDiaryEntry: ConfirmDiaryEntryUseCase,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val entryId = savedStateHandle.get<Long>(ConfirmEntryRoute.ARG_DIARY_ENTRY_ID) ?: 0L
    private val date = savedStateHandle.get<String>(ConfirmEntryRoute.ARG_DATE)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private val _state = MutableStateFlow(ConfirmEntryUiState(today = MealFormState.today(clock), form = MealFormState.now(clock)))
    val state: StateFlow<ConfirmEntryUiState> = _state.asStateFlow()

    private val _events = Channel<ConfirmEntryEvent>(Channel.BUFFERED)
    val events: Flow<ConfirmEntryEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Long?>(PickedFoodResult.KEY_ID, null).filterNotNull().collect { id ->
                val name = savedStateHandle.get<String>(PickedFoodResult.KEY_NAME)
                savedStateHandle[PickedFoodResult.KEY_ID] = null
                savedStateHandle[PickedFoodResult.KEY_NAME] = null
                if (id > 0 && !name.isNullOrBlank()) _state.update { it.copy(food = MealFood(id, name), foodNotResolved = false) }
            }
        }
        load()
    }

    fun onRetryLoad() = load()

    private fun load() {
        _state.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            val day = date?.takeIf { entryId > 0 }?.let { d -> user?.let { getDiaryDay(it.id.value, d) } }
            val entry = day?.getOrNull()?.entries?.firstOrNull { it.id.value == entryId }
            val proposedFoodId = entry?.proposedFoodId
            val name = entry?.foodName
            val grams = entry?.proposedGrams
            when {
                entry != null && !entry.isPendingConfirmation -> {
                    _state.update { it.copy(isLoading = false, showAlreadyConfirmed = true) }
                }
                entry != null && proposedFoodId != null && name != null && grams != null -> {
                    val food = MealFood(proposedFoodId, name)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            proposedFood = food,
                            food = food,
                            proposedGrams = grams,
                            form = it.form.copy(portionText = gramsInput(grams), mealTime = entry.localTimestamp.value),
                        )
                    }
                }
                else -> _state.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onPortionChange(text: String) {
        _state.update { it.copy(form = it.form.copy(portionText = text, showPortionError = false)) }
    }

    fun onPlanAnswer(inPlan: Boolean) {
        _state.update { it.copy(form = it.form.copy(inPlan = inPlan, showPlanError = false)) }
    }

    fun onChangeFood() {
        viewModelScope.launch { _events.send(ConfirmEntryEvent.PickFood) }
    }

    fun onDismissDialogs() = _state.update { it.copy(showError = false) }

    fun onAlreadyConfirmedAcknowledged() {
        _state.update { it.copy(showAlreadyConfirmed = false) }
        viewModelScope.launch { _events.send(ConfirmEntryEvent.Done) }
    }

    fun onConfirm() {
        val current = _state.value
        if (current.isSaving) return
        val proposedFood = current.proposedFood ?: return
        val proposedGrams = current.proposedGrams ?: return
        val food = current.food ?: proposedFood
        val validation = current.form.validate(Instant.now(clock), checkTime = false)
        _state.update { it.copy(form = validation.form, showError = false) }
        if (!validation.isValid) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = confirmDiaryEntry(
                entryId = DiaryEntryId(entryId),
                proposedFood = proposedFood,
                proposedGrams = proposedGrams,
                food = food,
                portion = requireNotNull(validation.portion),
                planAdherence = requireNotNull(validation.planAdherence),
            )
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false) }
                _events.send(ConfirmEntryEvent.Done)
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update {
                when {
                    error is DomainError.Conflict && error.code == CODE_ALREADY_CONFIRMED ->
                        it.copy(isSaving = false, showAlreadyConfirmed = true)
                    error is DomainError.Validation && error.code == CODE_FOOD_NOT_RESOLVED ->
                        it.copy(isSaving = false, foodNotResolved = true)
                    else -> it.copy(isSaving = false, showError = true)
                }
            }
        }
    }

    private fun gramsInput(grams: Double): String =
        if (grams % 1.0 == 0.0) grams.toLong().toString() else String.format(Locale.ROOT, "%.1f", grams)

    private companion object {
        const val CODE_ALREADY_CONFIRMED = "EstimateAlreadyConfirmed"
        const val CODE_FOOD_NOT_RESOLVED = "ReferenceFoodNotResolved"
    }
}
