package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Daily targets published by the nutritionist (`ActiveTargetsResource`): energy in kcal and macronutrients in
 * grams. It is the reduced contract: it never carries a diagnosis or the calculation basis (*Published Contract Only*).
 */
data class DailyTargets(
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
) {
    init {
        // The backend's PublishedContractOnly: a published version always has positive energy.
        require(energyKcal > 0) { "Energy target must be positive" }
        require(proteinG >= 0 && carbG >= 0 && fatG >= 0) { "Macro targets cannot be negative" }
    }

    /** Share of the daily energy each macronutrient provides (4/4/9 kcal per gram), between 0 and 1. */
    fun energyShareOf(macro: Macronutrient): Double {
        val kcal = when (macro) {
            Macronutrient.PROTEIN -> proteinG * KCAL_PER_G_PROTEIN
            Macronutrient.CARB -> carbG * KCAL_PER_G_CARB
            Macronutrient.FAT -> fatG * KCAL_PER_G_FAT
        }
        return (kcal / energyKcal).coerceIn(0.0, 1.0)
    }

    private companion object {
        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARB = 4.0
        const val KCAL_PER_G_FAT = 9.0
    }
}

/** The plan's macronutrients (`Protein`, `Carb`, `Fat` in `MacroChanged`). */
enum class Macronutrient(val code: String) {
    PROTEIN("Protein"),
    CARB("Carb"),
    FAT("Fat"),
    ;

    companion object {
        fun fromCode(code: String?): Macronutrient? = entries.firstOrNull { it.code == code }
    }
}
