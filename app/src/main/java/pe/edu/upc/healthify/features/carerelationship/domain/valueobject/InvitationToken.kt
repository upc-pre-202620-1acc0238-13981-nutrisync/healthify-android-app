package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Single-use invitation token: 32 random bytes in url-safe base64, 22–64 characters. It is a
 * secret: never logged or shown (`toString` hides it).
 *
 * The invitation QR code holds only the token. Anything without its shape is rejected on the phone with the same
 * message as `InvitationNotValid`, without spending a request.
 */
@JvmInline
value class InvitationToken(val value: String) {
    init {
        require(value.length in MIN_LENGTH..MAX_LENGTH) { "InvitationToken must have $MIN_LENGTH-$MAX_LENGTH chars" }
        require(value.all { it.isBase64UrlChar() }) { "InvitationToken must be base64 url-safe" }
    }

    override fun toString(): String = "InvitationToken(***)"

    companion object {
        const val MIN_LENGTH = 22
        const val MAX_LENGTH = 64

        /** The token read from the QR code, or `null` if the content does not look like an invitation. */
        fun parse(raw: String): InvitationToken? {
            val candidate = raw.trim()
            val valid = candidate.length in MIN_LENGTH..MAX_LENGTH && candidate.all { it.isBase64UrlChar() }
            return if (valid) InvitationToken(candidate) else null
        }

        private fun Char.isBase64UrlChar(): Boolean =
            this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_'
    }
}
