package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Period

/** Id de la consulta guiada (NC-2). */
@JvmInline
value class ConsultationId(val value: Long) {
    init {
        require(value > 0) { "ConsultationId must be positive" }
    }
}

/** Lee un número escrito por el profesional: acepta coma o punto decimal («74,2» o «74.2»). `null` si no es número. */
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

/** Resultado de leer un campo numérico con rango del backend. */
sealed interface NumberInput {
    data class Valid(val value: Double) : NumberInput
    data object Empty : NumberInput
    data object OutOfRange : NumberInput
}

/**
 * Rangos de plausibilidad del backend: talla (`HeightCm`, NC-1), medición (`ImplausibleMeasurement`, NC-3), hábitos
 * (`EatingHabits`) y bioquímicos (`BiochemistryPanel`). El teléfono los aplica antes de enviar (fail fast).
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

    /** Un texto vacío es [NumberInput.Empty]; uno que no es número o queda fuera del rango, [NumberInput.OutOfRange]. */
    fun parse(text: String): NumberInput {
        if (text.isBlank()) return NumberInput.Empty
        val value = DecimalText.parse(text) ?: return NumberInput.OutOfRange
        if (decimals == 0 && value != kotlin.math.floor(value)) return NumberInput.OutOfRange
        return if (contains(value)) NumberInput.Valid(DecimalText.round(value, decimals)) else NumberInput.OutOfRange
    }
}

/** Talla fija de los datos base (NC-1): 50–250 cm con 1 decimal. No se vuelve a pedir en cada consulta. */
@JvmInline
value class HeightCm(val value: Double) {
    init {
        require(ClinicalRange.HEIGHT_CM.contains(value)) { "Height must be between 50 and 250 cm" }
    }
}

/**
 * Fecha de nacimiento de los datos base (NC-1): entre 1 y 120 años antes de [today]. La edad se calcula sola (cumpleaños
 * exacto), nunca se guarda.
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

/** IMC calculado en vivo en EV-2 (NC-3): peso / talla², 1 decimal (redondeo «away from zero» como el backend). */
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

/** Razón obligatoria al escribir valores propios en EV-4 (`OverrideReasonRequired`): 1–500 caracteres. */
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

/** «Otra indicación» (NC-6): texto del profesional de 3 a 140 caracteres; se muestra tal cual, nunca se traduce. */
@JvmInline
value class CustomGuideline(val text: String) {
    init {
        require(text.trim().length in MIN_LENGTH..MAX_LENGTH) { "A custom guideline has 3 to 140 characters" }
    }

    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 140

        /** Máximo de indicaciones propias por versión del plan. */
        const val MAX_PER_VERSION = 5
    }
}

/** Mensaje para el paciente de la versión (NC-9): hasta 500 caracteres; vacío = sin mensaje. */
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
 * `Idempotency-Key` de la publicación (NC-2): ASCII visible, 1–64 caracteres. Se genera una vez por consulta y se
 * reusa en cada reintento, así un timeout seguido de «Volver a intentarlo» nunca publica dos versiones.
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
