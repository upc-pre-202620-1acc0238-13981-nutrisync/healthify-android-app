package pe.edu.upc.healthify.features.foodcatalog.domain.valueobject

/**
 * What the patient typed in "Search food", without extra spaces.
 *
 * Search starts at 2 characters ("qu" already filters the phone's catalog); the server only adds external
 * providers from 3, so with 2 the answer comes from the server's local catalog.
 */
@JvmInline
value class FoodSearchTerm private constructor(val value: String) {

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 80

        /** `null` if too short to search (the list stays empty, without "We couldn't find it"). */
        fun of(raw: String): FoodSearchTerm? {
            val trimmed = raw.trim().replace(WHITESPACE, " ")
            return if (trimmed.length < MIN_LENGTH) null else FoodSearchTerm(trimmed.take(MAX_LENGTH))
        }

        private val WHITESPACE = Regex("\\s+")
    }
}
