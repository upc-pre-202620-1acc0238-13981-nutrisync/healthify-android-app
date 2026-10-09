package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Restricción del plan (NC-6): un código de la lista cerrada, que la app traduce, o un texto libre anterior a la
 * lista cerrada (`legacyRestrictions`), que se muestra tal cual.
 */
sealed interface DietaryRestriction {

    data class Catalog(val code: RestrictionCode) : DietaryRestriction

    data class Legacy(val text: String) : DietaryRestriction {
        init {
            require(text.isNotBlank()) { "A legacy restriction needs text" }
        }
    }

    companion object {
        /** Un código desconocido se muestra tal cual (X-2). `null` si viene vacío. */
        fun ofCode(code: String?): DietaryRestriction? {
            val known = RestrictionCode.fromCode(code)
            return when {
                known != null -> Catalog(known)
                !code.isNullOrBlank() -> Legacy(code.trim())
                else -> null
            }
        }

        fun legacy(text: String?): DietaryRestriction? = text?.trim()?.takeIf { it.isNotEmpty() }?.let(::Legacy)
    }
}

/** Lista cerrada de restricciones del backend (`DietaryRestriction`, NC-6). */
enum class RestrictionCode(val code: String) {
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
        fun fromCode(code: String?): RestrictionCode? = entries.firstOrNull { it.code == code }
    }
}
