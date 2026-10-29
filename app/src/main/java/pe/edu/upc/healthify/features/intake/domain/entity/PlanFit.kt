package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence

/**
 * Sugerencia para «¿Esta comida estaba en tu plan?» (`PlanFitResource`, IN-8). La calcula el servidor con las
 * restricciones del plan y lo que queda hoy, sin enviar el plan a la IA. La app la **preselecciona** y el paciente la
 * puede cambiar: lo que se registra es siempre su respuesta.
 *
 * @param violatedRestrictions las restricciones del plan que el plato parece romper (vacía si ninguna).
 * @param remainingKcal lo que quedaba hoy antes de esta comida; `null` si no se mandó el día.
 */
data class PlanFit(
    val suggested: PlanAdherence,
    val violatedRestrictions: List<DietaryRestriction>,
    val reason: PlanFitReason,
    val energyKcal: Double,
    val remainingKcal: Double?,
) {
    init {
        require(suggested.isAnswered) { "A suggestion is in or off the plan" }
    }

    val suggestsInPlan: Boolean get() = suggested == PlanAdherence.IN_PLAN

    val breaksRestrictions: Boolean get() = violatedRestrictions.isNotEmpty()
}

/** Por qué se sugiere lo que se sugiere: cada uno con su propio texto. */
enum class PlanFitReason(val code: String) {
    FITS("Fits"),
    BREAKS_RESTRICTION("BreaksRestriction"),
    EXCEEDS_REMAINING("ExceedsRemaining"),
    ;

    companion object {
        fun fromCode(code: String?): PlanFitReason? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
