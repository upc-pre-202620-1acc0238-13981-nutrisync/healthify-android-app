package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * The moment the patient says they ate ("When did you eat?"), with the phone's offset. The patient declares it
 * and **nobody rewrites it**: not the app when resending, not the backend (*Declared Local Timestamp Never
 * Rewritten*). That is why the exact value is kept, offset included.
 *
 * Retroactive window rule (`Intake:RetroactiveLoggingWindowHours` = 48): a new log cannot be more than 48 h old
 * or in the future. The offline sync does not apply the window: queued entries are [restore]d without
 * validating it again.
 */
class LocalTimestamp private constructor(val value: OffsetDateTime) {

    /** The clinical day of the log: the one on the phone's clock when it was declared. */
    val localDate: LocalDate get() = value.toLocalDate()

    val instant: Instant get() = value.toInstant()

    override fun equals(other: Any?): Boolean = other is LocalTimestamp && other.value == value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    enum class Validity { VALID, TOO_OLD, IN_FUTURE }

    companion object {
        /** The backend's `Intake:RetroactiveLoggingWindowHours`. */
        val RETROACTIVE_WINDOW: Duration = Duration.ofHours(48)

        /** Tolerance for a time picked as "now" while the patient fills in the form. */
        private val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(5)

        fun validate(value: OffsetDateTime, now: Instant): Validity {
            val instant = value.toInstant()
            return when {
                instant.isAfter(now.plus(FUTURE_TOLERANCE)) -> Validity.IN_FUTURE
                instant.isBefore(now.minus(RETROACTIVE_WINDOW)) -> Validity.TOO_OLD
                else -> Validity.VALID
            }
        }

        /** A new log. @throws IllegalArgumentException outside the window. */
        fun forNewEntry(value: OffsetDateTime, now: Instant): LocalTimestamp {
            require(validate(value, now) == Validity.VALID) { "A new entry must be within the last 48 h" }
            return LocalTimestamp(value)
        }

        /** A moment already declared (offline queue, backend): it is kept as is. */
        fun restore(value: OffsetDateTime): LocalTimestamp = LocalTimestamp(value)
    }
}
