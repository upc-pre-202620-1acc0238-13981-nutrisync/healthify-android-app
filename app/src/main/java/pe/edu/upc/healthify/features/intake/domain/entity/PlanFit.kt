package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence

/**
 * Suggestion for "Was this meal in your plan?" (`PlanFitResource`). The server computes it from the plan
 * restrictions and what is left today, without sending the plan to the AI. The app **preselects** it and the patient
 * can change it: what gets logged is always their answer.
 *
 * @param violatedRestrictions the plan restrictions the dish seems to break (empty if none).
 * @param remainingKcal what was left today before this meal; `null` if the day was not sent.
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

/** Why the suggestion is what it is: each reason with its own text. */
enum class PlanFitReason(val code: String) {
    FITS("Fits"),
    BREAKS_RESTRICTION("BreaksRestriction"),
    EXCEEDS_REMAINING("ExceedsRemaining"),
    ;

    companion object {
        fun fromCode(code: String?): PlanFitReason? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
