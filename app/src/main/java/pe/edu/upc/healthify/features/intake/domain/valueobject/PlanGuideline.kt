package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Una indicación del plan (NC-6): un código del catálogo cerrado, que la app traduce, o un texto escrito por el
 * nutricionista, que **nunca** se traduce (dato clínico).
 */
sealed interface PlanGuideline {

    data class Catalog(val code: GuidelineCode) : PlanGuideline

    /** «Otra indicación» o un código que esta versión de la app no conoce: se muestra tal cual. */
    data class Custom(val text: String) : PlanGuideline {
        init {
            require(text.isNotBlank()) { "A custom guideline needs text" }
        }
    }

    companion object {
        /**
         * Lee un `GuidelineItem` (`{ code }` o `{ custom }`). Un código desconocido se trata como `Custom` con el
         * código como texto (X-2: «código desconocido → texto legado tal cual»). `null` si no trae nada.
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

/** Catálogo cerrado de indicaciones del backend (`Guideline`, NC-6). */
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
