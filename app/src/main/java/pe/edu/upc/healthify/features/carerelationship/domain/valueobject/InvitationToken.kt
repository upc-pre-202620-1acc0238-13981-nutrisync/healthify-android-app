package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Token de un solo uso de la invitación (F4): 32 bytes aleatorios en base64 url-safe, 22–64 caracteres. Es un
 * secreto: nunca se registra ni se muestra (`toString` lo oculta).
 *
 * DECISIÓN PT1: el QR de PR2 contiene solo el token. Lo que no tenga su forma se rechaza en el teléfono con el mismo
 * mensaje que `InvitationNotValid` (F5 caso 1), sin gastar una petición.
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

        /** El token leído del QR, o `null` si el contenido no tiene la forma de una invitación. */
        fun parse(raw: String): InvitationToken? {
            val candidate = raw.trim()
            val valid = candidate.length in MIN_LENGTH..MAX_LENGTH && candidate.all { it.isBase64UrlChar() }
            return if (valid) InvitationToken(candidate) else null
        }

        private fun Char.isBase64UrlChar(): Boolean =
            this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_'
    }
}
