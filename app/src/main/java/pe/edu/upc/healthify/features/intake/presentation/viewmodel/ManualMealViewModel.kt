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
 * PT9 → PT10 / PT10.1 → PT10.3. Compone FoodCatalog (búsqueda local-first y copia offline del catálogo) e Intake
 * (registro). Sin conexión la comida queda encolada con su `clientEntryId` y su hora local.
 */
@HiltViewModel
class ManualMealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAccountCreatedOn: GetAccountCreatedOnUseCase,
    private val searchFoods: SearchFoodsUseCase,
    private val refreshLocalFoodCatalog: RefreshLocalFoodCatalogUseCase,
    private val logManualMeal: LogManualMealUseCase,
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

    /** Uno por registro: los reintentos (PT10.3) reenvían el mismo y el backend no duplica la comida. */
    private val clientEntryId = ClientEntryId.random()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            getAccountCreatedOn(user.id).getOrNull()?.let { day -> _state.update { it.copy(earliestDay = day) } }
            // La copia del catálogo para buscar sin conexión (≤ 500); sin red se queda la que hay.
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

    /** PT10 «Buscar de nuevo». */
    fun onSearchAgain() = onQueryChange("")

    fun onFoodSelected(food: ReferenceFood) {
        _state.update { it.copy(selectedFood = food, foodError = null) }
    }

    fun onPortionChange(text: String) {
        _state.update { it.copy(form = it.form.copy(portionText = text, showPortionError = false)) }
    }

    fun onPlanAnswer(inPlan: Boolean) {
        _state.update { it.copy(form = it.form.copy(inPlan = inPlan, showPlanError = false)) }
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
    }

    /** «Registrar» (o «Elegir este alimento» en el modo de PT8). */
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

    /** PT10.3 «Volver a intentarlo»: vuelve al formulario con todo lo elegido. */
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
