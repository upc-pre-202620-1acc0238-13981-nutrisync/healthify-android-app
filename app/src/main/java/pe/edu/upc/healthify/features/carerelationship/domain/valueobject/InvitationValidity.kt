package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

import java.time.Duration
import java.time.Instant

/**
 * «Vence en» de PR2. El backend pide una fecha de expiración futura (`ExpirationDateRequired`); la app ofrece
 * vigencias fijas. DECISIÓN PR2: 1 hora, 24 horas (la del frame, por defecto) y 7 días.
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
