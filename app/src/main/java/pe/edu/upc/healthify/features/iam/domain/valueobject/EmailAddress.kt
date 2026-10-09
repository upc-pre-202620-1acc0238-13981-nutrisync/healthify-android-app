package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Correo de la cuenta, normalizado y validado como el VO `Email` del backend (F1): sin espacios, en minúsculas,
 * hasta 255 caracteres y con la forma `^[^@\s]+@[^@\s]+\.[^@\s]+$`.
 */
@JvmInline
value class EmailAddress private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 255

        private val Pattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        /** @throws IllegalArgumentException si [raw] no es un correo válido. */
        operator fun invoke(raw: String): EmailAddress {
            val normalized = raw.trim().lowercase()
            require(normalized.isNotEmpty() && normalized.length <= MAX_LENGTH) { "Invalid email length" }
            require(Pattern.matches(normalized)) { "Invalid email" }
            return EmailAddress(normalized)
        }

        /** El correo, o `null` si [raw] no es válido (validación de formularios sin excepciones). */
        fun parse(raw: String): EmailAddress? = try {
            invoke(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
