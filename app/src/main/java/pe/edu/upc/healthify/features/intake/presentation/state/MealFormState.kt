package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * The fields shared by the photo confirmation and manual logging: portion, "When did you eat?" and "Was it in your
 * plan?". Errors only show after trying to confirm (or if the backend rejects the moment).
 *
 * @param planSuggestion the server's suggestion for "Was it in your plan?", which is preselected.
 * @param planAnsweredByPatient the patient already tapped Yes/No: a new suggestion no longer changes their answer.
 */
data class MealFormState(
    val portionText: String = "",
    val mealTime: OffsetDateTime,
    val inPlan: Boolean? = null,
    val showPortionError: Boolean = false,
    val mealTimeError: LocalTimestamp.Validity? = null,
    val showPlanError: Boolean = false,
    val showTimePicker: Boolean = false,
    val planSuggestion: PlanFit? = null,
    val planAnsweredByPatient: Boolean = false,
) {
    /** The patient picked Yes/No (even if it matches the suggestion): from now on their answer wins. */
    fun answerPlan(inPlan: Boolean): MealFormState =
        copy(inPlan = inPlan, planAnsweredByPatient = true, showPlanError = false)

    /**
     * A suggestion arrives (or goes away). While the patient has not answered, the answer follows the
     * suggestion: it is preselected, and with no suggestion it goes back to unanswered.
     */
    fun withPlanSuggestion(fit: PlanFit?): MealFormState {
        if (planAnsweredByPatient) return copy(planSuggestion = fit)
        return copy(planSuggestion = fit, inPlan = fit?.suggestsInPlan, showPlanError = showPlanError && fit == null)
    }

    /** The patient picked the opposite of the suggestion. */
    val planSuggestionOverridden: Boolean
        get() = planSuggestion != null && inPlan != null && inPlan != planSuggestion.suggestsInPlan

    /**
     * Applies the value object rules (portion 1–2000 g, 48 h window, Yes/No answer) and makes the errors
     * visible. With [checkTime] `false` the moment is kept as is (an entry already logged is not rewritten).
     */
    fun validate(now: Instant, checkTime: Boolean = true): MealFormValidation {
        val portion = PortionGrams.parseOrNull(portionText)
        val validity = if (checkTime) LocalTimestamp.validate(mealTime, now) else LocalTimestamp.Validity.VALID
        val timestamp = when {
            validity != LocalTimestamp.Validity.VALID -> null
            checkTime -> LocalTimestamp.forNewEntry(mealTime, now)
            else -> LocalTimestamp.restore(mealTime)
        }
        val adherence = inPlan?.let(PlanAdherence::fromAnswer)
        return MealFormValidation(
            form = copy(
                showPortionError = portion == null,
                mealTimeError = validity.takeIf { it != LocalTimestamp.Validity.VALID },
                showPlanError = adherence == null,
            ),
            portion = portion,
            timestamp = timestamp,
            planAdherence = adherence,
        )
    }

    companion object {
        fun now(clock: Clock, portionText: String = ""): MealFormState =
            MealFormState(portionText = portionText, mealTime = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))

        /** The date and time the patient picked, with the offset the phone had at that moment. */
        fun offsetOf(dateTime: LocalDateTime, clock: Clock): OffsetDateTime = dateTime.atZone(clock.zone).toOffsetDateTime()

        fun today(clock: Clock): LocalDate = LocalDate.now(clock)
    }
}

/** Result of [MealFormState.validate]: the values already built (or `null` if their rule failed). */
data class MealFormValidation(
    val form: MealFormState,
    val portion: PortionGrams?,
    val timestamp: LocalTimestamp?,
    val planAdherence: PlanAdherence?,
) {
    val isValid: Boolean get() = portion != null && timestamp != null && planAdherence != null
}
