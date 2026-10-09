package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Porción en gramos que el paciente confirma o escribe (PT8, PT10.1). El backend exige > 0; la app además pone un
 * techo de 2000 g, el mismo que acepta para una estimación por foto (IN-7): cubre un plato servido y caza un cero de
 * más. No juzga la porción (*ningún juicio sobre la porción*): solo descarta lo que no puede ser un dato.
 */
@JvmInline
value class PortionGrams(val value: Double) {
    init {
        require(value.isFinite() && value >= MIN && value <= MAX) { "A portion must be between $MIN and $MAX g" }
    }

    companion object {
        const val MIN = 1.0
        const val MAX = 2000.0

        private val NUMBER = Regex("^\\d{1,4}([.,]\\d{1,2})?$")

        /** «150», «150,5» o «150.5». `null` si no es un número o está fuera de rango. */
        fun parseOrNull(text: String): PortionGrams? {
            val trimmed = text.trim()
            if (!NUMBER.matches(trimmed)) return null
            val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
            return if (value in MIN..MAX) PortionGrams(value) else null
        }

        /** Una cifra del backend (propuesta de la IA, ingrediente de una idea) si cumple la regla. */
        fun ofOrNull(value: Double?): PortionGrams? =
            value?.takeIf { it.isFinite() && it in MIN..MAX }?.let(::PortionGrams)
    }
}
