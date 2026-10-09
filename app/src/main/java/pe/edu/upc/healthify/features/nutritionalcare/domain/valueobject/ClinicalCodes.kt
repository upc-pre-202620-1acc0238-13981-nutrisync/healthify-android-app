package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

/*
 * Listas cerradas de NutritionalCare (NC-1, NC-3, NC-4, NC-6). Cada valor lleva el código exacto del backend; la app
 * traduce los códigos con strings.xml (X-2) y un código desconocido se descarta o se muestra tal cual, según la pantalla.
 */

/** Sexo biológico de los datos base (NC-1): solo Femenino y Masculino. */
enum class BiologicalSex(val code: String) {
    FEMALE("Female"),
    MALE("Male"),
    ;

    companion object {
        fun fromCode(code: String?): BiologicalSex? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Antecedentes médicos (NC-1): la lista vacía es válida (no hay «Ninguno»). */
enum class MedicalCondition(val code: String) {
    TYPE_2_DIABETES("Type2Diabetes"),
    HYPERTENSION("Hypertension"),
    CELIAC_DISEASE("CeliacDisease"),
    HYPOTHYROIDISM("Hypothyroidism"),
    CHRONIC_KIDNEY_DISEASE("ChronicKidneyDisease"),
    GOUT("Gout"),
    ;

    companion object {
        fun fromCode(code: String?): MedicalCondition? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Actividad física actual (NC-3) con su factor por defecto (`NutritionalCare:ActivityFactors`). */
enum class ActivityLevel(val code: String, val defaultFactor: Double) {
    SEDENTARY("Sedentary", 1.2),
    LIGHT("Light", 1.375),
    MODERATE("Moderate", 1.55),
    INTENSE("Intense", 1.725),
    ;

    companion object {
        fun fromCode(code: String?): ActivityLevel? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Casillas del protocolo de medición (NC-3): al menos una marcada. */
enum class ProtocolCheck(val code: String) {
    FASTING("Fasting"),
    NO_SHOES("NoShoes"),
    LIGHT_CLOTHING("LightClothing"),
    EMPTY_BLADDER("EmptyBladder"),
    SAME_SCALE("SameScale"),
    ;

    companion object {
        fun fromCode(code: String?): ProtocolCheck? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/**
 * Diagnóstico nutricional (NC-4), lista cerrada sin «Otro». Comparte los cortes de la OMS con la categoría del IMC:
 * < 18,5 · 18,5–24,9 · 25–29,9 · 30–34,9 · 35–39,9 · ≥ 40.
 */
enum class DiagnosisCode(val code: String) {
    UNDERWEIGHT("Underweight"),
    NORMAL_WEIGHT("NormalWeight"),
    OVERWEIGHT_GRADE_I("OverweightGradeI"),
    OBESITY_GRADE_I("ObesityGradeI"),
    OBESITY_GRADE_II("ObesityGradeII"),
    OBESITY_GRADE_III("ObesityGradeIII"),
    ;

    companion object {
        fun fromCode(code: String?): DiagnosisCode? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        /** `BodyMassIndex.CategoryFor` del backend. */
        fun forBmi(bmi: Double): DiagnosisCode = when {
            bmi < 18.5 -> UNDERWEIGHT
            bmi < 25.0 -> NORMAL_WEIGHT
            bmi < 30.0 -> OVERWEIGHT_GRADE_I
            bmi < 35.0 -> OBESITY_GRADE_I
            bmi < 40.0 -> OBESITY_GRADE_II
            else -> OBESITY_GRADE_III
        }
    }
}

/**
 * Quién eligió el diagnóstico (NC-4). Aceptar la sugerencia determinista («por regla») cuenta como
 * [PRACTITIONER_SELECTED]: solo una generación real de IA se registra como aceptada.
 */
enum class DiagnosisSource(val code: String) {
    AI_SUGGESTION_ACCEPTED("AiSuggestionAccepted"),
    PRACTITIONER_SELECTED("PractitionerSelected"),
    ;

    companion object {
        fun fromCode(code: String?): DiagnosisSource? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** De dónde sale una sugerencia de EV-3/EV-5 (IA-6, IA-7): la IA o la regla fija cuando la IA no está disponible. */
enum class SuggestionSource(val code: String) {
    AI("Ai"),
    RULE("Rule"),
    ;

    companion object {
        /** Cualquier otro valor se lee como regla: nunca se rotula «IA» lo que no lo es. */
        fun fromCode(code: String?): SuggestionSource = if (code.equals(AI.code, ignoreCase = true)) AI else RULE
    }
}

/** Paso de la consulta guiada (NC-2): EV-2 a EV-5. */
enum class ConsultationStep(val code: String, val number: Int) {
    MEASUREMENT("Measurement", 1),
    DIAGNOSIS("Diagnosis", 2),
    TARGETS("Targets", 3),
    PUBLICATION("Publication", 4),
    ;

    companion object {
        const val TOTAL = 4

        fun fromCode(code: String?): ConsultationStep? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }

        fun fromNumber(number: Int): ConsultationStep? = entries.firstOrNull { it.number == number }
    }
}

/** Restricciones del plan (NC-6), lista cerrada sin «Otra restricción». */
enum class PlanRestrictionCode(val code: String) {
    LACTOSE_FREE("LactoseFree"),
    GLUTEN_FREE("GlutenFree"),
    VEGAN("Vegan"),
    VEGETARIAN("Vegetarian"),
    TREE_NUT_FREE("TreeNutFree"),
    SHELLFISH_FREE("ShellfishFree"),
    KOSHER("Kosher"),
    HALAL("Halal"),
    ;

    companion object {
        fun fromCode(code: String?): PlanRestrictionCode? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Indicaciones del catálogo (NC-6). Las que no están aquí van como «Otra indicación» (texto del profesional). */
enum class PlanGuidelineCode(val code: String) {
    PRIORITIZE_VEGETABLES("PrioritizeVegetables"),
    DRINK_2L_WATER("Drink2LWater"),
    AVOID_SUGARY_DRINKS("AvoidSugaryDrinks"),
    PROTEIN_AT_BREAKFAST("ProteinAtBreakfast"),
    REDUCE_SALT("ReduceSalt"),
    EAT_EVERY_3_TO_4_HOURS("EatEvery3To4Hours"),
    PROTEIN_AND_VEGETABLES_AT_DINNER("ProteinAndVegetablesAtDinner"),
    ;

    companion object {
        fun fromCode(code: String?): PlanGuidelineCode? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Ecuación del gasto basal (F9). KatchMcArdle necesita la grasa corporal de la medición. */
enum class EnergyEquation(val code: String) {
    MIFFLIN_ST_JEOR("MifflinStJeor"),
    HARRIS_BENEDICT("HarrisBenedict"),
    FAO_WHO_UNU("FaoWhoUnu"),
    KATCH_MCARDLE("KatchMcArdle"),
    ;

    companion object {
        fun fromCode(code: String?): EnergyEquation? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Tipo de déficit (F9): kcal fijas (0–1500) o porcentaje del gasto total (0–40). */
enum class DeficitKind(val code: String, val maximum: Double) {
    FIXED_KCAL("FixedKcal", 1500.0),
    PERCENT_OF_TDEE("PercentOfTdee", 40.0),
    ;

    companion object {
        fun fromCode(code: String?): DeficitKind? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
