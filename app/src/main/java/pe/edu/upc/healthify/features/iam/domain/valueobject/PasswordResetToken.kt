package pe.edu.upc.healthify.features.iam.domain.valueobject

/**
 * Token the backend returns when the code is verified: sets the new password once and expires within a few
 * minutes. Lives in memory only; never stored or logged.
 */
@JvmInline
value class PasswordResetToken(val value: String) {

    init {
        require(value.isNotBlank()) { "Reset token is required" }
    }

    override fun toString(): String = "PasswordResetToken(****)"
}
