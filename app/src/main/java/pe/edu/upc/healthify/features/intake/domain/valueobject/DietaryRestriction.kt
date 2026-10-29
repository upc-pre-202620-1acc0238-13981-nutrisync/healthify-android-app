package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * A plan restriction: a code from the closed list, which the app translates, or free text written before the
 * closed list existed (`legacyRestrictions`), which is shown as is.
 */
sealed interface DietaryRestriction {

    data class Catalog(val code: RestrictionCode) : DietaryRestriction

    data class Legacy(val text: String) : DietaryRestriction {
        init {
            require(text.isNotBlank()) { "A legacy restriction needs text" }
        }
    }

    companion object {
        /** An unknown code is shown as is. `null` if it is empty. */
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

/** The backend's closed list of restrictions (`DietaryRestriction`). */
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
