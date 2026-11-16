package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * What the patient typed by hand as an invitation code, judged as they type.
 *
 * The code is the same single-use token the QR carries ([InvitationToken]): 22–64 base64 url-safe characters. When
 * someone dictates or copies it, spaces and line breaks slip in (the nutritionist's screen shows it in groups of
 * four), so every whitespace is ignored. Upper and lower case are **not** interchangeable: the token is case sensitive.
 *
 * The typed value is a secret like the token: this type never carries it as text ([toString] of [Valid] hides it).
 */
sealed interface InvitationCodeInput {
    /** Nothing typed yet (or only spaces). */
    data object Empty : InvitationCodeInput

    /** Valid characters so far, but fewer than [InvitationToken.MIN_LENGTH]. */
    data class TooShort(val length: Int) : InvitationCodeInput

    /** More than [InvitationToken.MAX_LENGTH] characters. */
    data object TooLong : InvitationCodeInput

    /** A character that a token never has. */
    data object InvalidCharacters : InvitationCodeInput

    /** Ready to redeem. */
    data class Valid(val token: InvitationToken) : InvitationCodeInput

    companion object {
        /** [raw] without any whitespace: what is validated and sent. */
        fun normalize(raw: String): String = raw.filterNot { it.isWhitespace() }

        fun evaluate(raw: String): InvitationCodeInput {
            val code = normalize(raw)
            return when {
                code.isEmpty() -> Empty
                !code.all { it.isBase64UrlChar() } -> InvalidCharacters
                code.length > InvitationToken.MAX_LENGTH -> TooLong
                code.length < InvitationToken.MIN_LENGTH -> TooShort(code.length)
                else -> Valid(InvitationToken(code))
            }
        }

        private fun Char.isBase64UrlChar(): Boolean =
            this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_'
    }
}
