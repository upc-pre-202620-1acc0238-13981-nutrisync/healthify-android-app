package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * A plan guideline: a code from the closed catalog, which the app translates, or text written by the
 * nutritionist, which is **never** translated (clinical data).
 */
sealed interface PlanGuideline {

    data class Catalog(val code: GuidelineCode) : PlanGuideline

    /** "Other guideline" or a code this app version does not know: it is shown as is. */
    data class Custom(val text: String) : PlanGuideline {
        init {
            require(text.isNotBlank()) { "A custom guideline needs text" }
        }
    }

    companion object {
        /**
         * Reads a `GuidelineItem` (`{ code }` or `{ custom }`). An unknown code is treated as `Custom` with the
         * code as its text, shown as is. `null` if it carries nothing.
         */
        fun of(code: String?, custom: String?): PlanGuideline? {
            val known = GuidelineCode.fromCode(code)
            return when {
                known != null -> Catalog(known)
                !custom.isNullOrBlank() -> Custom(custom.trim())
                !code.isNullOrBlank() -> Custom(code.trim())
                else -> null
            }
        }
    }
}

/** The backend's closed catalog of guidelines (`Guideline`). */
enum class GuidelineCode(val code: String) {
    PRIORITIZE_VEGETABLES("PrioritizeVegetables"),
    DRINK_2L_WATER("Drink2LWater"),
    AVOID_SUGARY_DRINKS("AvoidSugaryDrinks"),
    PROTEIN_AT_BREAKFAST("ProteinAtBreakfast"),
    REDUCE_SALT("ReduceSalt"),
    EAT_EVERY_3_TO_4_HOURS("EatEvery3To4Hours"),
    PROTEIN_AND_VEGETABLES_AT_DINNER("ProteinAndVegetablesAtDinner"),
    ;

    companion object {
        fun fromCode(code: String?): GuidelineCode? = entries.firstOrNull { it.code == code }
    }
}
