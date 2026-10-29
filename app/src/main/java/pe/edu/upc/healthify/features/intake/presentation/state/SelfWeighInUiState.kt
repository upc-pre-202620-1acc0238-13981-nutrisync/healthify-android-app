package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Self weigh-in (with its out-of-range and error states). Errors only show after trying to save
 * (or if the backend rejects the value).
 *
 * @param fasted "Did you weigh yourself fasting?" **with no default value** (`null` = not answered).
 * @param saveFailed the error screen with "Try again" (what was typed is kept).
 */
data class SelfWeighInUiState(
    val today: LocalDate,
    /** The day the account was created: the day picker does not offer earlier days. */
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
    /** Saved or queued: back to the trend screen, which shows the Snackbar. */
    data class Saved(val outcome: SelfWeighInOutcome) : SelfWeighInEvent
}
