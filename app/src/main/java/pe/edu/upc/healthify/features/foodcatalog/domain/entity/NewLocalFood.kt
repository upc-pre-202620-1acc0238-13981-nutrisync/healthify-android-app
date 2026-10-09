package pe.edu.upc.healthify.features.foodcatalog.domain.entity

/**
 * Alimento local que agrega un nutricionista (`CreateLocalOverrideResource`, F14c): platos peruanos que el catálogo
 * externo no trae. Queda marcado como local **de por vida**: una importación nunca lo sobrescribe.
 *
 * Mismas reglas que el backend (`LocalName`, `NutrientsPer100g`): nombre de 1–200 caracteres con al menos una letra,
 * energía 0–950 kcal y cada macro 0–100 g por 100 g.
 */
data class NewLocalFood(
    val name: String,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double,
    val carbGPer100g: Double,
    val fatGPer100g: Double,
) {
    init {
        require(name.isNotBlank() && name.trim().length <= MAX_NAME_LENGTH) { "A local food has a 1-200 character name" }
        require(name.any(Char::isLetter)) { "A local food name has letters" }
        require(energyKcalPer100g in 0.0..MAX_ENERGY_KCAL) { "Energy per 100 g is 0-950 kcal" }
        require(listOf(proteinGPer100g, carbGPer100g, fatGPer100g).all { it in 0.0..MAX_MACRO_G }) {
            "Each macro per 100 g is 0-100 g"
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 200
        const val MAX_ENERGY_KCAL = 950.0
        const val MAX_MACRO_G = 100.0
    }
}

/** Campo de PR15.1 vacío o fuera de rango. */
enum class LocalFoodField { NAME, ENERGY, PROTEIN, CARB, FAT }

/** Validación de PR15.1 antes de enviar (`400 LocalNameAndNutrientsRequired` en el backend). */
sealed interface LocalFoodValidation {
    data class Valid(val food: NewLocalFood) : LocalFoodValidation
    data class Invalid(val fields: Set<LocalFoodField>) : LocalFoodValidation

    companion object {
        fun of(name: String, energy: String, protein: String, carb: String, fat: String): LocalFoodValidation {
            val trimmedName = name.trim()
            val energyValue = parse(energy)?.takeIf { it in 0.0..NewLocalFood.MAX_ENERGY_KCAL }
            val proteinValue = parse(protein)?.takeIf { it in 0.0..NewLocalFood.MAX_MACRO_G }
            val carbValue = parse(carb)?.takeIf { it in 0.0..NewLocalFood.MAX_MACRO_G }
            val fatValue = parse(fat)?.takeIf { it in 0.0..NewLocalFood.MAX_MACRO_G }
            val invalid = buildSet {
                val nameOk = trimmedName.isNotEmpty() && trimmedName.length <= NewLocalFood.MAX_NAME_LENGTH &&
                    trimmedName.any(Char::isLetter)
                if (!nameOk) add(LocalFoodField.NAME)
                if (energyValue == null) add(LocalFoodField.ENERGY)
                if (proteinValue == null) add(LocalFoodField.PROTEIN)
                if (carbValue == null) add(LocalFoodField.CARB)
                if (fatValue == null) add(LocalFoodField.FAT)
            }
            if (invalid.isNotEmpty() || energyValue == null || proteinValue == null || carbValue == null || fatValue == null) {
                return Invalid(invalid)
            }
            return Valid(NewLocalFood(trimmedName, energyValue, proteinValue, carbValue, fatValue))
        }

        /** «180», «12,5» o «12.5»; `null` si está vacío o no es un número. */
        private fun parse(text: String): Double? =
            text.trim().replace(',', '.').takeIf(String::isNotEmpty)?.toDoubleOrNull()?.takeIf(Double::isFinite)
    }
}
