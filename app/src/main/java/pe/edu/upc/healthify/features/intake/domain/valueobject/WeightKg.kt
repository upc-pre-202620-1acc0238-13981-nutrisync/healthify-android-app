package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Weight of a self weigh-in in kilograms. The backend's *Plausible Weight Range* rule (`WeightKg`): between 20 and
 * 400 kg, rounded to 2 decimals. Outside that range the backend answers `400 ImplausibleWeightValue`; the
 * phone catches it first (the "check this number, it looks out of range" error). It does not judge the value: it
 * only rejects what cannot be a reading (an extra zero, a slipped finger).
 */
@JvmInline
value class WeightKg private constructor(val value: Double) {

    /** What the patient typed in "Weight (kg)". */
    sealed interface Input {
        data class Valid(val weight: WeightKg) : Input

        /** Empty field. */
        data object Empty : Input

        /** Not a number with up to 3 integer digits and 2 decimals, or outside 20–400 kg. */
        data object OutOfRange : Input
    }

    companion object {
        const val MIN = 20.0
        const val MAX = 400.0
        private const val SCALE = 2

        private val NUMBER = Regex("^\\d{1,3}([.,]\\d{1,2})?$")

        /** @throws IllegalArgumentException outside 20–400 kg or if it is not finite. */
        fun of(value: Double): WeightKg {
            require(value.isFinite() && value in MIN..MAX) { "A weight reading must be between $MIN and $MAX kg" }
            return WeightKg(BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP).toDouble())
        }

        /** "68,4", "68.4" or "68". */
        fun parse(text: String): Input {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return Input.Empty
            if (!NUMBER.matches(trimmed)) return Input.OutOfRange
            val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return Input.OutOfRange
            return if (value in MIN..MAX) Input.Valid(of(value)) else Input.OutOfRange
        }

        /** A value already saved (offline queue) if it meets the rule. */
        fun ofOrNull(value: Double?): WeightKg? = value?.takeIf { it.isFinite() && it in MIN..MAX }?.let(::of)
    }
}
