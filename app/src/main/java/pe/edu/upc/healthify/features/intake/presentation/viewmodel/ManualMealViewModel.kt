package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.RefreshLocalFoodCatalogUseCase
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.SearchFoodsUseCase
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogManualMealUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.presentation.navigation.ManualMealRoute
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealEvent
import pe.edu.upc.healthify.features.intake.presentation.state.ManualMealUiState
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Manual logging, from search to result. Composes FoodCatalog (local-first search and offline catalog copy) and
 * Intake (logging). Offline the meal is queued with its `clientEntryId` and its local time.
 */
@HiltViewModel
class ManualMealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAccountCreatedOn: GetAccountCreatedOnUseCase,
    private val searchFoods: SearchFoodsUseCase,
    private val refreshLocalFoodCatalog: RefreshLocalFoodCatalogUseCase,
    private val logManualMeal: LogManualMealUseCase,
    private val planFitSuggester: PlanFitSuggester,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ManualMealUiState(
            today = MealFormState.today(clock),
            form = MealFormState.now(clock),
            pickFoodOnly = savedStateHandle.get<Boolean>(ManualMealRoute.ARG_PICK_FOOD_ONLY) ?: false,
        ),
    )
    val state: StateFlow<ManualMealUiState> = _state.asStateFlow()

    private val _events = Channel<ManualMealEvent>(Channel.BUFFERED)
    val events: Flow<ManualMealEvent> = _events.receiveAsFlow()

    /** One per log: retries resend the same one and the backend does not duplicate the meal. */
    private val clientEntryId = ClientEntryId.random()
    private var searchJob: Job? = null
    private var patientId: Long? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            patientId = user.id.value
            getAccountCreatedOn(user.id).getOrNull()?.let { day -> _state.update { it.copy(earliestDay = day) } }
            // The catalog copy for offline search (≤ 500); without network the existing one stays.
            refreshLocalFoodCatalog(user.id.value)
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, foodError = null) }
        searchJob?.cancel()
        val term = FoodSearchTerm.of(query)
        if (term == null) {
            _state.update { it.copy(results = emptyList(), searchedTerm = null, isSearching = false, localOnly = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchFoods(term).collect { result ->
                _state.update { state ->
                    state.copy(
                        results = result.foods,
                        searchedTerm = result.term.value,
                        isSearching = result.serverPending && result.foods.isEmpty(),
                        localOnly = result.localOnly,
                        selectedFood = state.selectedFood?.takeIf { selected -> result.foods.any { it.id == selected.id } },
                    )
                }
            }
        }
    }

    /** "Search again". */
    fun onSearchAgain() = onQueryChange("")

    fun onFoodSelected(food: ReferenceFood) {
        _state.update { it.copy(selectedFood = food, foodError = null) }
        requestPlanFit()
    }

    fun onPortionChange(text: String) {
        _state.update { it.copy(form = it.form.copy(portionText = text, showPortionError = false)) }
        requestPlanFit()
    }

    fun onPlanAnswer(inPlan: Boolean) {
        _state.update { it.copy(form = it.form.answerPlan(inPlan)) }
    }

    /** The "Was it in your plan?" suggestion follows the picked food, portion and day. */
    private fun requestPlanFit() {
        val current = _state.value
        val pid = patientId ?: return
        if (current.pickFoodOnly) return
        val food = current.selectedFood?.let { MealFood(it.id.value, it.name) }
        planFitSuggester.request(viewModelScope, pid, food, current.form) { fit ->
            _state.update { it.copy(form = it.form.withPlanSuggestion(fit)) }
        }
    }

    fun onMealTimeClick() = _state.update { it.copy(form = it.form.copy(showTimePicker = true)) }

    fun onMealTimeDismiss() = _state.update { it.copy(form = it.form.copy(showTimePicker = false)) }

    fun onMealTimeSelected(dateTime: LocalDateTime) {
        val value = MealFormState.offsetOf(dateTime, clock)
        val validity = LocalTimestamp.validate(value, Instant.now(clock))
        _state.update {
            it.copy(
                form = it.form.copy(
                    mealTime = value,
                    showTimePicker = false,
                    mealTimeError = validity.takeIf { v -> v != LocalTimestamp.Validity.VALID },
                ),
            )
        }
        requestPlanFit()
    }

    /** "Log" (or "Pick this food" in pick mode). */
    fun onSubmit() {
        val current = _state.value
        if (current.isSaving) return
        val food = current.selectedFood
        if (current.pickFoodOnly) {
            if (food == null) {
                _state.update { it.copy(foodError = ManualMealUiState.FoodError.NOT_CHOSEN) }
            } else {
                viewModelScope.launch { _events.send(ManualMealEvent.FoodPicked(food.id.value, food.name)) }
            }
            return
        }
        val validation = current.form.validate(Instant.now(clock))
        _state.update {
            it.copy(form = validation.form, foodError = if (food == null) ManualMealUiState.FoodError.NOT_CHOSEN else null)
        }
        if (food == null || !validation.isValid) return
        val meal = NewManualMeal(
            food = MealFood(food.id.value, food.name),
            portion = requireNotNull(validation.portion),
            localTimestamp = requireNotNull(validation.timestamp),
            planAdherence = requireNotNull(validation.planAdherence),
            clientEntryId = clientEntryId,
        )
        save(meal)
    }

    /** "Try again": back to the form with everything picked. */
    fun onRetry() {
        _state.update { it.copy(saveFailed = false) }
        onSubmit()
    }

    private fun save(meal: NewManualMeal) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _state.update { it.copy(isSaving = false) }
                return@launch
            }
            val result = logManualMeal(user.id.value, meal)
            val outcome = result.getOrNull()
            if (outcome != null) {
                _state.update { it.copy(isSaving = false) }
                _events.send(ManualMealEvent.Logged(outcome))
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update {
                when ((error as? DomainError.Validation)?.code) {
                    CODE_WINDOW_EXCEEDED ->
                        it.copy(isSaving = false, form = it.form.copy(mealTimeError = LocalTimestamp.Validity.TOO_OLD))
                    CODE_FOOD_NOT_RESOLVED ->
                        it.copy(isSaving = false, foodError = ManualMealUiState.FoodError.NOT_RESOLVED)
                    else -> it.copy(isSaving = false, saveFailed = true)
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CODE_WINDOW_EXCEEDED = "RetroactiveLoggingWindowExceeded"
        const val CODE_FOOD_NOT_RESOLVED = "ReferenceFoodNotResolved"
    }
}
