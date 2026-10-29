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
import pe.edu.upc.healthify.features.iam.application.usecase.GetAccountCreatedOnUseCase
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.RecordSelfWeighInUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInEvent
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInUiState
import pe.edu.upc.healthify.features.intake.presentation.state.SelfWeighInUiState.WeightError
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Self weigh-in. Offline the self weigh-in is queued with its `clientEntryId` and its local time and it
 * goes back to the trend screen just like on success ("same toast, stays pending").
 *
 * The backend puts no window on self weigh-ins (it only requires the moment); the phone applies the
 * 48 h one the design asks for ("You can log up to 48 h back"), the same `LocalTimestamp` rule as the diary.
 */
@HiltViewModel
class SelfWeighInViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getAccountCreatedOn: GetAccountCreatedOnUseCase,
    private val recordSelfWeighIn: RecordSelfWeighInUseCase,
    connectivityObserver: ConnectivityObserver,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SelfWeighInUiState(
            today = MealFormState.today(clock),
            weighedAt = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES),
        ),
    )
    val state: StateFlow<SelfWeighInUiState> = _state.asStateFlow()

    private val _events = Channel<SelfWeighInEvent>(Channel.BUFFERED)
    val events: Flow<SelfWeighInEvent> = _events.receiveAsFlow()

    /** One per self weigh-in: retries and the queue resend the same one and the backend does not duplicate it. */
    private val clientEntryId = ClientEntryId.random()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            getAccountCreatedOn(user.id).getOrNull()?.let { day -> _state.update { it.copy(earliestDay = day) } }
        }
    }

    fun onWeightChange(text: String) {
        _state.update { it.copy(weightText = text, weightError = null) }
    }

    fun onFastedAnswer(fasted: Boolean) {
        _state.update { it.copy(fasted = fasted, showFastedError = false) }
    }

    fun onTimeClick() = _state.update { it.copy(showTimePicker = true) }

    fun onTimeDismiss() = _state.update { it.copy(showTimePicker = false) }

    fun onTimeSelected(dateTime: LocalDateTime) {
        val value = MealFormState.offsetOf(dateTime, clock)
        val validity = LocalTimestamp.validate(value, Instant.now(clock))
        _state.update {
            it.copy(
                weighedAt = value,
                showTimePicker = false,
                timeError = validity.takeIf { v -> v != LocalTimestamp.Validity.VALID },
            )
        }
    }

    /** "Save": applies the value object rules and shows all errors at once. */
    fun onSave() {
        val current = _state.value
        if (current.isSaving) return
        val input = WeightKg.parse(current.weightText)
        val validity = LocalTimestamp.validate(current.weighedAt, Instant.now(clock))
        _state.update {
            it.copy(
                weightError = when (input) {
                    is WeightKg.Input.Valid -> null
                    WeightKg.Input.Empty -> WeightError.EMPTY
                    WeightKg.Input.OutOfRange -> WeightError.OUT_OF_RANGE
                },
                timeError = validity.takeIf { v -> v != LocalTimestamp.Validity.VALID },
                showFastedError = it.fasted == null,
            )
        }
        val fasted = current.fasted
        if (input !is WeightKg.Input.Valid || validity != LocalTimestamp.Validity.VALID || fasted == null) return
        save(
            NewSelfWeighIn(
                weight = input.weight,
                localTimestamp = LocalTimestamp.forNewEntry(current.weighedAt, Instant.now(clock)),
                fasted = fasted,
                clientEntryId = clientEntryId,
            ),
        )
    }

    /** "Try again": back to the form with what was typed, and tries again. */
    fun onRetry() {
        _state.update { it.copy(saveFailed = false) }
        onSave()
    }

    private fun save(weighIn: NewSelfWeighIn) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val user = observeCurrentUser().first()
            if (user == null) {
                _state.update { it.copy(isSaving = false) }
                return@launch
            }
            val result = recordSelfWeighIn(user.id.value, weighIn)
            val outcome = result.getOrNull()
            if (outcome != null) {
                _state.update { it.copy(isSaving = false) }
                _events.send(SelfWeighInEvent.Saved(outcome))
                return@launch
            }
            val error = result.domainErrorOrNull()
            _state.update {
                when ((error as? DomainError.Validation)?.code) {
                    // The backend has the last word on the range.
                    CODE_IMPLAUSIBLE_WEIGHT -> it.copy(isSaving = false, weightError = WeightError.OUT_OF_RANGE)
                    else -> it.copy(isSaving = false, saveFailed = true)
                }
            }
        }
    }

    private companion object {
        const val CODE_IMPLAUSIBLE_WEIGHT = "ImplausibleWeightValue"
    }
}
