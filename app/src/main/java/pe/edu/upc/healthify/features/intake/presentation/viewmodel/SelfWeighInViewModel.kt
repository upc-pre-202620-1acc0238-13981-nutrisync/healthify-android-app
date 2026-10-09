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
 * PT12 · Autopesaje. Sin conexión el autopesaje queda encolado con su `clientEntryId` y su hora local (IN-4) y se
 * vuelve a PT13 igual que con éxito («mismo toast, queda pendiente»).
 *
 * DECISIÓN PT12: el backend no pone ventana a los autopesajes (F19 solo exige el momento); el teléfono aplica la de
 * 48 h que pide el frame («Puedes registrar hasta 48 h hacia atrás»), la misma regla de `LocalTimestamp` del diario.
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

    /** Uno por autopesaje: los reintentos (PT12.2) y la cola reenvían el mismo y el backend no lo duplica. */
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

    /** «Guardar»: aplica las reglas de los value objects y muestra todos los errores a la vez. */
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

    /** PT12.2 «Volver a intentarlo»: vuelve al formulario con lo escrito y lo intenta otra vez. */
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
                    // PT12.E: el backend tiene la última palabra sobre el rango.
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
