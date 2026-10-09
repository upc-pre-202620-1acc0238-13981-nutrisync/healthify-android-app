package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams

/** PT10.1 · una comida escrita a mano: es una confirmación desde el principio (F17). */
data class NewManualMeal(
    val food: MealFood,
    val portion: PortionGrams,
    val localTimestamp: LocalTimestamp,
    val planAdherence: PlanAdherence,
    val clientEntryId: ClientEntryId,
) {
    init {
        require(planAdherence.isAnswered) { "A manual meal needs the plan answer (PlanAdherenceRequired)" }
    }
}

/** Qué hizo el paciente con la estimación de la foto en PT7/PT8 (IN-2). */
sealed interface PhotoConfirmation {
    /** «Sí, es correcto». */
    data object AsProposed : PhotoConfirmation

    /** Otro plato (una alternativa o uno del catálogo) y/u otros gramos. La propuesta se conserva al lado. */
    data class Adjusted(val food: MealFood, val portion: PortionGrams) : PhotoConfirmation
}

/** PT7/PT8 · comida por foto ya confirmada (`photo-logs` con `analysisId`, IN-7). */
data class NewPhotoMeal(
    val analysis: MealPhotoAnalysis,
    val confirmation: PhotoConfirmation,
    val localTimestamp: LocalTimestamp,
    val planAdherence: PlanAdherence,
    val clientEntryId: ClientEntryId,
) {
    init {
        require(planAdherence.isAnswered) { "A confirmed photo meal needs the plan answer (PlanAdherenceRequired)" }
    }

    /** Lo que queda registrado: el plato y los gramos confirmados. */
    val loggedFood: MealFood
        get() = when (confirmation) {
            PhotoConfirmation.AsProposed -> analysis.food
            is PhotoConfirmation.Adjusted -> confirmation.food
        }

    val loggedGrams: Double
        get() = when (confirmation) {
            PhotoConfirmation.AsProposed -> analysis.estimatedGrams
            is PhotoConfirmation.Adjusted -> confirmation.portion.value
        }
}

/** Un alimento de una comida de varios (`manual-logs/batch`, IN-6). */
data class MealGroupItem(val food: MealFood, val portion: PortionGrams, val clientEntryId: ClientEntryId)

/** PT14.5 «Registrar esta comida»: N entradas `Manual` con el mismo momento, en una sola transacción. */
data class NewMealGroup(
    val items: List<MealGroupItem>,
    val localTimestamp: LocalTimestamp,
    val mealIdeaId: String?,
    val description: String,
    val planAdherence: PlanAdherence = PlanAdherence.IN_PLAN,
) {
    init {
        require(items.size in 1..MAX_ITEMS) { "A meal group has 1 to $MAX_ITEMS foods (InvalidMealGroupItems)" }
        require(items.map { it.clientEntryId }.toSet().size == items.size) { "Each food has its own clientEntryId" }
        require(planAdherence.isAnswered) { "A meal group needs the plan answer" }
    }

    companion object {
        const val MAX_ITEMS = 10
    }
}

/** Cómo terminó un registro. */
enum class MealLogOutcome {
    /** El backend lo guardó. */
    LOGGED,

    /** Sin conexión: quedó en la cola del teléfono y se enviará solo («Pendiente de enviar»). */
    QUEUED,
}

/**
 * Aviso para el Snackbar de PT14 al volver al diario (PT14.1): «Comida registrada: Lomo saltado · 320 g» o
 * «Guardado. Se enviará cuando tengas conexión.».
 */
data class LoggedMealNotice(
    val description: String,
    val grams: Double?,
    val outcome: MealLogOutcome,
)
