package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Recovery code that arrives by email: exactly six digits. The backend decides whether it is correct;
 * here we only avoid sending something that cannot be.
 */
@JvmInline
value class PasswordResetCode private constructor(val value: String) {

    /** Never expose the code in logs or in UI state. */
    override fun toString(): String = "PasswordResetCode(******)"

    companion object {
        const val LENGTH = 6

        /** The code, or `null` if [raw] (without spaces) is not six digits. */
        fun parse(raw: String): PasswordResetCode? {
            val digits = raw.filterNot { it.isWhitespace() }
            return if (digits.length == LENGTH && digits.all { it in '0'..'9' }) PasswordResetCode(digits) else null
        }
    }
}
