package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * PT12 · Autopesaje (+ PT12.E fuera de rango, PT12.2 error). Los errores solo se muestran después de intentar guardar
 * (o si el backend rechaza el valor).
 *
 * @param fasted «¿Te pesaste en ayunas?» **sin valor por defecto** (`null` = sin responder).
 * @param saveFailed PT12.2: la pantalla de error con «Volver a intentarlo» (lo escrito se conserva).
 */
data class SelfWeighInUiState(
    val today: LocalDate,
    /** Día en que se creó la cuenta: el selector del día no ofrece días anteriores. */
    val earliestDay: LocalDate? = null,
    val weightText: String = "",
    val weighedAt: OffsetDateTime,
    val fasted: Boolean? = null,
    val weightError: WeightError? = null,
    val timeError: LocalTimestamp.Validity? = null,
    val showFastedError: Boolean = false,
    val showTimePicker: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val isOffline: Boolean = false,
) {
    enum class WeightError { EMPTY, OUT_OF_RANGE }
}

sealed interface SelfWeighInEvent {
    /** Guardado o encolado: vuelve a PT13, que muestra el Snackbar (PT13.1). */
    data class Saved(val outcome: SelfWeighInOutcome) : SelfWeighInEvent
}
