package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * "Was this meal in your plan?". It is answered on confirming and does not change afterwards. "No" marks the entry
 * as "Off plan" and it **still counts** in the log. [NOT_ANSWERED] only exists on "To confirm" or historical
 * entries: a new log always carries an answer (`PlanAdherenceRequired`).
 */
enum class PlanAdherence(val code: String) {
    IN_PLAN("InPlan"),
    OFF_PLAN("OffPlan"),
    NOT_ANSWERED("NotAnswered"),
    ;

    val isAnswered: Boolean get() = this != NOT_ANSWERED

    companion object {
        fun fromAnswer(inPlan: Boolean): PlanAdherence = if (inPlan) IN_PLAN else OFF_PLAN

        /** An unknown value reads as not answered: no answer is made up. */
        fun fromCode(code: String?): PlanAdherence =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NOT_ANSWERED
    }
}
