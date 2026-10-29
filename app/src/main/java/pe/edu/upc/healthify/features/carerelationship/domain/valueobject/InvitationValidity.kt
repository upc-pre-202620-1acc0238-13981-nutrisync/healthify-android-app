package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

import java.time.Duration
import java.time.Instant

/**
 * "Expires in" on the invite screen. The backend asks for a future expiration date (`ExpirationDateRequired`); the
 * app offers fixed durations: 1 hour, 24 hours (the design's value, the default) and 7 days.
 */
enum class InvitationValidity(val duration: Duration) {
    ONE_HOUR(Duration.ofHours(1)),
    ONE_DAY(Duration.ofHours(24)),
    SEVEN_DAYS(Duration.ofDays(7)),
    ;

    fun expiresAt(now: Instant): Instant = now.plus(duration)

    companion object {
        val DEFAULT = ONE_DAY
    }
}
