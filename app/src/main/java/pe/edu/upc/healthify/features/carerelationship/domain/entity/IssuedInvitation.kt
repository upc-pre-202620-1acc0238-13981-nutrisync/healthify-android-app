package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import java.time.Duration
import java.time.Instant

/**
 * The newly issued invitation. The token arrives **only once**, in this response: it is not stored on the
 * phone or requested again (ethical rule of the invite screen: never promise "you can see it later").
 */
data class IssuedInvitation(
    val invitationId: Long,
    val token: InvitationToken,
    val expiresAt: Instant,
) {
    init {
        require(invitationId > 0) { "InvitationId must be positive" }
    }

    /** Remaining validity at [now] (never negative). */
    fun remaining(now: Instant): Duration =
        Duration.between(now, expiresAt).takeIf { !it.isNegative } ?: Duration.ZERO

    fun isExpired(now: Instant): Boolean = !now.isBefore(expiresAt)
}
