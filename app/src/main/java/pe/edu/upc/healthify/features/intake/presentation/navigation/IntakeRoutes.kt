package pe.edu.upc.healthify.features.intake.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * PT5 → PT6 → PT6.1 → PT7 / PT7.2 / PT7.3 → PT8: registro por foto en una sola ruta (los pasos son estados de un
 * mismo ViewModel). Con [pendingPhotoId] arranca analizando una foto tomada sin conexión.
 */
@Serializable
data class MealPhotoRoute(val pendingPhotoId: String? = null) {
    companion object {
        const val ARG_PENDING_PHOTO_ID = "pendingPhotoId"
    }
}

/**
 * PT9 / PT9.K / PT10 / PT10.1 / PT10.3 · registrar a mano. Con [pickFoodOnly] solo se elige un alimento y se
 * devuelve a la pantalla anterior (PT8 «¿No es este plato? Toca para cambiarlo.»).
 */
@Serializable
data class ManualMealRoute(val pickFoodOnly: Boolean = false) {
    companion object {
        const val ARG_PICK_FOOD_ONLY = "pickFoodOnly"
    }
}

/** PT8 de una entrada «Por confirmar» del diario: la entrada se relee del día [date] (`2026-09-09`). */
@Serializable
data class ConfirmEntryRoute(val diaryEntryId: Long, val date: String) {
    companion object {
        const val ARG_DIARY_ENTRY_ID = "diaryEntryId"
        const val ARG_DATE = "date"
    }
}

/** PT14.4 Ideas para hoy → PT14.5 Detalle (dos estados de la misma ruta). */
@Serializable
data object MealIdeasRoute

/** PT12 · Autopesaje (se abre desde PT13 «Registrar autopesaje»). */
@Serializable
data object SelfWeighInRoute

/** PT19 · Pendientes por sincronizar. */
@Serializable
data object PendingSyncRoute

/** Resultado de [ManualMealRoute] con `pickFoodOnly`: se deja en el `SavedStateHandle` de la pantalla anterior. */
object PickedFoodResult {
    const val KEY_ID = "picked_food_id"
    const val KEY_NAME = "picked_food_name"
}

/** PT22 · Recordatorios (+ PT22.M), desde PT21. */
@Serializable
data object RemindersRoute
