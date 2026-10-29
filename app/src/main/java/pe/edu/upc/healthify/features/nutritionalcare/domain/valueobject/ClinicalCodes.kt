package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

/*
 * NutritionalCare closed lists. Each value carries the backend's exact code; the app
 * translates codes through strings.xml, and an unknown code is dropped or shown as is, depending on the screen.
 */

/** Biological sex in the baseline: only Female and Male. */
enum class BiologicalSex(val code: String) {
    FEMALE("Female"),
    MALE("Male"),
    ;

    companion object {
        fun fromCode(code: String?): BiologicalSex? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Medical history: an empty list is valid (there is no "None" option). */
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

/** Current physical activity with its default factor (`NutritionalCare:ActivityFactors`). */
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

/** Measurement protocol checkboxes: at least one checked. */
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
 * Nutritional diagnosis, a closed list with no "Other". It shares the WHO cut-offs with the BMI category:
 * < 18.5 · 18.5–24.9 · 25–29.9 · 30–34.9 · 35–39.9 · ≥ 40.
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

        /** The backend's `BodyMassIndex.CategoryFor`. */
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
 * Who chose the diagnosis. Accepting the deterministic ("rule-based") suggestion counts as
 * [PRACTITIONER_SELECTED]: only a real AI generation is recorded as accepted.
 */
enum class DiagnosisSource(val code: String) {
    AI_SUGGESTION_ACCEPTED("AiSuggestionAccepted"),
    PRACTITIONER_SELECTED("PractitionerSelected"),
    ;

    companion object {
        fun fromCode(code: String?): DiagnosisSource? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Where a diagnosis or guideline suggestion comes from: the AI, or the fixed rule when AI is unavailable. */
enum class SuggestionSource(val code: String) {
    AI("Ai"),
    RULE("Rule"),
    ;

    companion object {
        /** Any other value reads as a rule: nothing that is not AI is ever labeled "AI". */
        fun fromCode(code: String?): SuggestionSource = if (code.equals(AI.code, ignoreCase = true)) AI else RULE
    }
}

/** Step of the guided consultation. */
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

/** Plan restrictions, a closed list with no "Other restriction". */
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

/** Catalog guidelines. Anything not listed here goes as "Other guideline" (the practitioner's text). */
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

/** Basal expenditure equation. KatchMcArdle needs the body fat from the measurement. */
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

/** Deficit type: fixed kcal (0–1500) or a percentage of total expenditure (0–40). */
enum class DeficitKind(val code: String, val maximum: Double) {
    FIXED_KCAL("FixedKcal", 1500.0),
    PERCENT_OF_TDEE("PercentOfTdee", 40.0),
    ;

    companion object {
        fun fromCode(code: String?): DeficitKind? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
