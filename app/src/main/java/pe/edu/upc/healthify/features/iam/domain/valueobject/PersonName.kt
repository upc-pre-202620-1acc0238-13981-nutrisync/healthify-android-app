package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Nombres y apellidos (IAM-1). Los apellidos pueden venir vacíos: las cuentas anteriores a IAM-1 quedaron con la
 * parte local del correo como nombre y sin apellidos. Una cuenta nueva se valida con [forSignUp], que aplica
 * además las reglas del VO `PersonName` del backend.
 */
data class PersonName(val givenNames: String, val familyNames: String) {

    init {
        require(givenNames.isNotBlank()) { "Given names are required" }
        require(givenNames == givenNames.trim() && familyNames == familyNames.trim()) { "Names must be trimmed" }
        require(givenNames.length <= MAX_LENGTH && familyNames.length <= MAX_LENGTH) { "Name too long" }
    }

    /** «María José» → «María», para «Hola, María». */
    val firstGivenName: String get() = givenNames.split(' ').first()

    val fullName: String get() = if (familyNames.isEmpty()) givenNames else "$givenNames $familyNames"

    /** Inicial del avatar. */
    val initial: Char get() = givenNames.first().uppercaseChar()

    /** Por qué una parte del nombre no sirve para una cuenta nueva. */
    enum class Problem { REQUIRED, INVALID_CHARACTERS, TOO_LONG }

    companion object {
        const val MAX_LENGTH = 80

        private val InnerWhitespace = Regex("\\s+")

        fun of(givenNames: String, familyNames: String): PersonName =
            PersonName(givenNames.trim(), familyNames.trim())

        /**
         * Nombre de una cuenta nueva (S3), con las reglas del backend: ambas partes obligatorias, espacios internos
         * colapsados, hasta 80 caracteres y sin dígitos ni `<` `>`.
         *
         * @throws IllegalArgumentException si alguna parte no cumple ([problemOf] dice por qué).
         */
        fun forSignUp(givenNames: String, familyNames: String): PersonName {
            require(problemOf(givenNames) == null && problemOf(familyNames) == null) { "Invalid name" }
            return PersonName(normalize(givenNames), normalize(familyNames))
        }

        /** El problema de una parte del nombre de una cuenta nueva, o `null` si sirve. */
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
