package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Peso de un autopesaje en kilogramos (PT12). Regla *Plausible Weight Range* del backend (`WeightKg`): entre 20 y
 * 400 kg, redondeado a 2 decimales. Fuera de ese rango el backend responde `400 ImplausibleWeightValue`; el
 * teléfono lo detecta antes (PT12.E «Revisa este número, parece fuera de rango»). No juzga el valor: solo descarta lo
 * que no puede ser una lectura (un cero de más, un dedo resbalado).
 */
@JvmInline
value class WeightKg private constructor(val value: Double) {

    /** Lo que escribió el paciente en «Peso (kg)». */
    sealed interface Input {
        data class Valid(val weight: WeightKg) : Input

        /** Campo vacío. */
        data object Empty : Input

        /** No es un número de hasta 3 enteros y 2 decimales, o está fuera de 20–400 kg. */
        data object OutOfRange : Input
    }

    companion object {
        const val MIN = 20.0
        const val MAX = 400.0
        private const val SCALE = 2

        private val NUMBER = Regex("^\\d{1,3}([.,]\\d{1,2})?$")

        /** @throws IllegalArgumentException fuera de 20–400 kg o si no es finito. */
        fun of(value: Double): WeightKg {
            require(value.isFinite() && value in MIN..MAX) { "A weight reading must be between $MIN and $MAX kg" }
            return WeightKg(BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP).toDouble())
        }

        /** «68,4», «68.4» o «68». */
        fun parse(text: String): Input {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return Input.Empty
            if (!NUMBER.matches(trimmed)) return Input.OutOfRange
            val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return Input.OutOfRange
            return if (value in MIN..MAX) Input.Valid(of(value)) else Input.OutOfRange
        }

        /** Un valor ya guardado (cola offline) si cumple la regla. */
        fun ofOrNull(value: Double?): WeightKg? = value?.takeIf { it.isFinite() && it in MIN..MAX }?.let(::of)
    }
}
