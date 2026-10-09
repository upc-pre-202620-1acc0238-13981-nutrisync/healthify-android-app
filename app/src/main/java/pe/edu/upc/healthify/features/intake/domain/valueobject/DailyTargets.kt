package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Metas diarias publicadas por el nutricionista (`ActiveTargetsResource`): energía en kcal y macronutrientes en
 * gramos. Es el contrato reducido: nunca trae diagnóstico ni base de cálculo (*Published Contract Only*).
 */
data class DailyTargets(
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
) {
    init {
        // PublishedContractOnly del backend: una versión publicada siempre trae energía positiva.
        require(energyKcal > 0) { "Energy target must be positive" }
        require(proteinG >= 0 && carbG >= 0 && fatG >= 0) { "Macro targets cannot be negative" }
    }

    /** Parte de la energía diaria que aporta cada macronutriente (4/4/9 kcal por gramo), entre 0 y 1. */
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

/** Macronutrientes del plan (`Protein`, `Carb`, `Fat` en `MacroChanged`). */
enum class Macronutrient(val code: String) {
    PROTEIN("Protein"),
    CARB("Carb"),
    FAT("Fat"),
    ;

    companion object {
        fun fromCode(code: String?): Macronutrient? = entries.firstOrNull { it.code == code }
    }
}
