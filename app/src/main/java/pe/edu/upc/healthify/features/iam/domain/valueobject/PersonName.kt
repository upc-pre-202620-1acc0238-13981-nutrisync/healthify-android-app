package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Given and family names. Family names may be empty: accounts created before names were required kept the
 * local part of the email as the given name and no family names. A new account is validated with [forSignUp],
 * which also applies the rules of the backend `PersonName` VO.
 */
data class PersonName(val givenNames: String, val familyNames: String) {

    init {
        require(givenNames.isNotBlank()) { "Given names are required" }
        require(givenNames == givenNames.trim() && familyNames == familyNames.trim()) { "Names must be trimmed" }
        require(givenNames.length <= MAX_LENGTH && familyNames.length <= MAX_LENGTH) { "Name too long" }
    }

    /** «María José» → «María», for «Hola, María». */
    val firstGivenName: String get() = givenNames.split(' ').first()

    val fullName: String get() = if (familyNames.isEmpty()) givenNames else "$givenNames $familyNames"

    /** Avatar initial. */
    val initial: Char get() = givenNames.first().uppercaseChar()

    /** Why a name part does not work for a new account. */
    enum class Problem { REQUIRED, INVALID_CHARACTERS, TOO_LONG }

    companion object {
        const val MAX_LENGTH = 80

        private val InnerWhitespace = Regex("\\s+")

        fun of(givenNames: String, familyNames: String): PersonName =
            PersonName(givenNames.trim(), familyNames.trim())

        /**
         * Name of a new account, with the backend rules: both parts required, inner spaces
         * collapsed, up to 80 characters and no digits or `<` `>`.
         *
         * @throws IllegalArgumentException if a part does not meet them ([problemOf] says why).
         */
        fun forSignUp(givenNames: String, familyNames: String): PersonName {
            require(problemOf(givenNames) == null && problemOf(familyNames) == null) { "Invalid name" }
            return PersonName(normalize(givenNames), normalize(familyNames))
        }

        /** The problem with a name part of a new account, or `null` if it is fine. */
        fun problemOf(part: String): Problem? {
            val normalized = normalize(part)
            return when {
                normalized.isEmpty() -> Problem.REQUIRED
                normalized.length > MAX_LENGTH -> Problem.TOO_LONG
                normalized.any { it.isDigit() || it == '<' || it == '>' } -> Problem.INVALID_CHARACTERS
                else -> null
            }
        }

        private fun normalize(part: String): String = part.trim().replace(InnerWhitespace, " ")
    }
}
