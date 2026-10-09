package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import java.time.LocalDate

/**
 * PT8 de una entrada «Por confirmar» (F16).
 *
 * @param proposedFood / [proposedGrams] lo que propuso la IA (se conserva al lado de lo confirmado).
 * @param food el plato elegido (el propuesto u otro del catálogo).
 * @param showAlreadyConfirmed «Esta comida ya estaba confirmada.» (`409 EstimateAlreadyConfirmed`) → vuelve al diario.
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

    /** Confirmada (o ya lo estaba): vuelve al diario, que la muestra como «Confirmada por ti». */
    data object Done : ConfirmEntryEvent
}
