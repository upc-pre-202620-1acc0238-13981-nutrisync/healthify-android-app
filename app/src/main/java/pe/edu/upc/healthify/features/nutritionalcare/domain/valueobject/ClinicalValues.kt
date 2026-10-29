package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Period

/** Guided consultation id. */
@JvmInline
value class ConsultationId(val value: Long) {
    init {
        require(value > 0) { "ConsultationId must be positive" }
    }
}

/** Parses a number typed by the practitioner: decimal comma or point ("74,2" or "74.2"). `null` if not a number. */
object DecimalText {
    fun parse(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized.count { it == '.' } > 1) return null
        if (!normalized.all { it.isDigit() || it == '.' }) return null
        return normalized.toDoubleOrNull()
    }

    fun round(value: Double, decimals: Int): Double =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
}

/** Result of reading a numeric field with a backend range. */
sealed interface NumberInput {
    data class Valid(val value: Double) : NumberInput
    data object Empty : NumberInput
    data object OutOfRange : NumberInput
}

/**
 * Backend plausibility ranges: height (`HeightCm`), measurement (`ImplausibleMeasurement`), habits
 * (`EatingHabits`) and biochemistry (`BiochemistryPanel`). The phone applies them before sending (fail fast).
 */
enum class ClinicalRange(val minimum: Double, val maximum: Double, val decimals: Int) {
    HEIGHT_CM(50.0, 250.0, 1),
    WEIGHT_KG(20.0, 350.0, 2),
    WAIST_CM(40.0, 200.0, 1),
    BODY_FAT_PERCENT(3.0, 70.0, 1),
    MEALS_PER_DAY(1.0, 10.0, 0),
    WATER_LITERS_PER_DAY(0.0, 10.0, 2),
    MEALS_OUT_PER_WEEK(0.0, 21.0, 0),
    GLUCOSE_MG_DL(20.0, 600.0, 1),
    TOTAL_CHOLESTEROL_MG_DL(50.0, 500.0, 1),
    TRIGLYCERIDES_MG_DL(20.0, 2000.0, 1),
    ;

    fun contains(value: Double): Boolean = value in minimum..maximum

    /** Empty text is [NumberInput.Empty]; text that is not a number or is out of range is [NumberInput.OutOfRange]. */
    fun parse(text: String): NumberInput {
        if (text.isBlank()) return NumberInput.Empty
        val value = DecimalText.parse(text) ?: return NumberInput.OutOfRange
        if (decimals == 0 && value != kotlin.math.floor(value)) return NumberInput.OutOfRange
        return if (contains(value)) NumberInput.Valid(DecimalText.round(value, decimals)) else NumberInput.OutOfRange
    }
}

/** Fixed baseline height: 50–250 cm with 1 decimal. It is not asked again at each consultation. */
@JvmInline
value class HeightCm(val value: Double) {
    init {
        require(ClinicalRange.HEIGHT_CM.contains(value)) { "Height must be between 50 and 250 cm" }
    }
}

/**
 * Baseline birth date: between 1 and 120 years before [today]. Age is calculated on the fly (exact birthday),
 * never stored.
 */
data class BirthDate(val value: LocalDate, val today: LocalDate) {
    init {
        require(isPlausible(value, today)) { "Birth date must be between 1 and 120 years ago" }
    }

    val ageYears: Int get() = ageOn(value, today)

    companion object {
        fun isPlausible(date: LocalDate, today: LocalDate): Boolean =
            !date.isAfter(today.minusYears(1)) && !date.isBefore(today.minusYears(120))

        fun ageOn(date: LocalDate, today: LocalDate): Int = Period.between(date, today).years
    }
}

/** BMI calculated live on the measurement step: weight / height², 1 decimal (rounded away from zero like the backend). */
data class BodyMassIndex(val value: Double) {
    init {
        require(value > 0) { "BMI must be positive" }
    }

    val category: DiagnosisCode get() = DiagnosisCode.forBmi(value)

    companion object {
        fun of(weightKg: Double, heightCm: Double): BodyMassIndex {
            require(weightKg > 0 && heightCm > 0) { "Weight and height must be positive" }
            val meters = heightCm / 100.0
            return BodyMassIndex(DecimalText.round(weightKg / (meters * meters), 1))
        }
    }
}

/** Required reason when entering own values on the targets step (`OverrideReasonRequired`): 1–500 characters. */
@JvmInline
value class OverrideReason(val value: String) {
    init {
        require(value.isNotBlank()) { "An override reason is required" }
        require(value.trim().length <= MAX_LENGTH) { "An override reason has at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 500
    }
}

/** "Other guideline": practitioner text of 3 to 140 characters; shown as is, never translated. */
@JvmInline
value class CustomGuideline(val text: String) {
    init {
        require(text.trim().length in MIN_LENGTH..MAX_LENGTH) { "A custom guideline has 3 to 140 characters" }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 140

        /** Maximum custom guidelines per plan version. */
        const val MAX_PER_VERSION = 5
    }
}

/** The version's message to the patient: up to 500 characters; empty = no message. */
@JvmInline
value class PatientMessage(val text: String) {
    init {
        require(text.isNotBlank()) { "A message cannot be empty" }
        require(text.trim().length <= MAX_LENGTH) { "A message has at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH = 500

        fun ofOptional(text: String?): PatientMessage? = text?.trim()?.takeIf { it.isNotEmpty() }?.let(::PatientMessage)
    }
}

/**
 * Publication `Idempotency-Key`: visible ASCII, 1–64 characters. Generated once per consultation and
 * reused on every retry, so a timeout followed by "Try again" never publishes two versions.
 */
@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(value.length in 1..MAX_LENGTH) { "An idempotency key has 1 to $MAX_LENGTH characters" }
        require(value.all { it in '!'..'~' }) { "An idempotency key holds visible ASCII only" }
    }

    companion object {
        const val MAX_LENGTH = 64

        fun random(): IdempotencyKey = IdempotencyKey(java.util.UUID.randomUUID().toString())
    }
}
