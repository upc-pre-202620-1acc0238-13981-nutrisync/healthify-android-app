package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Momento en que el paciente dice que comió («¿Cuándo comiste?»), con el offset del teléfono. Lo declara el
 * paciente y **nadie lo reescribe**: ni la app al reenviar, ni el backend (*Declared Local Timestamp Never
 * Rewritten*). Por eso se conserva el valor exacto, offset incluido.
 *
 * Regla de la ventana retroactiva (F15–F17, `Intake:RetroactiveLoggingWindowHours` = 48): un registro nuevo no puede
 * ser de hace más de 48 h ni del futuro. La sincronización offline no aplica la ventana (F20): lo encolado se
 * [restore]a sin volver a validarla.
 */
class LocalTimestamp private constructor(val value: OffsetDateTime) {

    /** El día clínico del registro: el del reloj del teléfono cuando se declaró. */
    val localDate: LocalDate get() = value.toLocalDate()

    val instant: Instant get() = value.toInstant()

    override fun equals(other: Any?): Boolean = other is LocalTimestamp && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    enum class Validity { VALID, TOO_OLD, IN_FUTURE }

    companion object {
        /** `Intake:RetroactiveLoggingWindowHours` del backend. */
        val RETROACTIVE_WINDOW: Duration = Duration.ofHours(48)

        /** Tolerancia para la hora elegida «ahora» mientras el paciente completa el formulario. */
        private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(5)

        fun validate(value: OffsetDateTime, now: Instant): Validity {
            val instant = value.toInstant()
            return when {
                instant.isAfter(now.plus(FUTURE_TOLERANCE)) -> Validity.IN_FUTURE
                instant.isBefore(now.minus(RETROACTIVE_WINDOW)) -> Validity.TOO_OLD
                else -> Validity.VALID
            }
        }

        /** Un registro nuevo (PT8, PT10.1, PT14.5). @throws IllegalArgumentException fuera de la ventana. */
        fun forNewEntry(value: OffsetDateTime, now: Instant): LocalTimestamp {
            require(validate(value, now) == Validity.VALID) { "A new entry must be within the last 48 h" }
            return LocalTimestamp(value)
        }

        /** Un momento ya declarado (cola offline, backend): se respeta tal cual. */
        fun restore(value: OffsetDateTime): LocalTimestamp = LocalTimestamp(value)
    }
}
