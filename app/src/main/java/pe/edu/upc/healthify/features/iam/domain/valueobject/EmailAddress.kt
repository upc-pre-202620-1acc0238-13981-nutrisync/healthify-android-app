package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Account email, normalized and validated like the backend `Email` VO: no spaces, lowercase,
 * up to 255 characters and shaped like `^[^@\s]+@[^@\s]+\.[^@\s]+$`.
 */
@JvmInline
value class EmailAddress private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 255

        private val Pattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        /** @throws IllegalArgumentException if [raw] is not a valid email. */
        operator fun invoke(raw: String): EmailAddress {
            val normalized = raw.trim().lowercase()
            require(normalized.isNotEmpty() && normalized.length <= MAX_LENGTH) { "Invalid email length" }
            require(Pattern.matches(normalized)) { "Invalid email" }
            return EmailAddress(normalized)
        }

        /** The email, or `null` if [raw] is not valid (form validation without exceptions). */
        fun parse(raw: String): EmailAddress? = try {
            invoke(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
