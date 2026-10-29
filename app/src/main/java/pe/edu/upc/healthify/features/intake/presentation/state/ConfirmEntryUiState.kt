package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import java.time.LocalDate

/**
 * Confirmation of a diary entry left "To confirm".
 *
 * @param proposedFood / [proposedGrams] what the AI proposed (kept alongside what is confirmed).
 * @param food the chosen dish (the proposed one or another from the catalog).
 * @param showAlreadyConfirmed "This meal was already confirmed." (`409 EstimateAlreadyConfirmed`) → diary.
 */
data class ConfirmEntryUiState(
    val today: LocalDate,
    val form: MealFormState,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val proposedFood: MealFood? = null,
    val proposedGrams: Double? = null,
    val food: MealFood? = null,
    val foodNotResolved: Boolean = false,
    val isSaving: Boolean = false,
    val showAlreadyConfirmed: Boolean = false,
    val showError: Boolean = false,
    val isOffline: Boolean = false,
)

sealed interface ConfirmEntryEvent {
    data object PickFood : ConfirmEntryEvent

    /** Confirmed (or it already was): back to the diary, which shows it as "Confirmed by you". */
    data object Done : ConfirmEntryEvent
}
