package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Password for a new account, with the rules of the backend `Password` VO (`WeakPassword`): 8 to 128 characters,
 * with at least one uppercase letter, one lowercase letter, one digit and one character that is not a letter or digit.
 *
 * Only used at sign-up: sign-in sends whatever the person typed (see [Credentials]).
 */
@JvmInline
value class Password private constructor(val value: String) {

    /** Never expose the password in logs or in UI state. */
    override fun toString(): String = "Password(****)"

    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 128

        /** @throws IllegalArgumentException if [raw] does not meet the rules. */
        operator fun invoke(raw: String): Password {
            require(isStrong(raw)) { "Weak password" }
            return Password(raw)
        }

        /** The password, or `null` if [raw] does not meet the rules. */
        fun parse(raw: String): Password? = if (isStrong(raw)) Password(raw) else null

        fun isStrong(raw: String): Boolean =
            raw.isNotBlank() &&
                raw.length in MIN_LENGTH..MAX_LENGTH &&
                raw.any { it.isUpperCase() } &&
                raw.any { it.isLowerCase() } &&
                raw.any { it.isDigit() } &&
                !raw.all { it.isLetterOrDigit() }
    }
}
