package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import java.time.Duration
import java.time.Instant

/**
 * PR2 · la invitación recién emitida (F4). El token llega **una sola vez**, en esta respuesta: no se guarda en el
 * teléfono ni se vuelve a pedir (regla ética de PR2: no se promete «podrás verlo después»).
 */
data class IssuedInvitation(
    val invitationId: Long,
    val token: InvitationToken,
    val expiresAt: Instant,
) {
    init {
        require(invitationId > 0) { "InvitationId must be positive" }
    }

    /** Lo que le queda de vigencia en [now] (nunca negativo). */
    fun remaining(now: Instant): Duration =
        Duration.between(now, expiresAt).takeIf { !it.isNegative } ?: Duration.ZERO

    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)
}
