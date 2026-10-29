package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Portion in grams the patient confirms or types. The backend requires > 0; the app also sets a ceiling of
 * 2000 g, the same one it accepts for a photo estimate: it covers a served plate and catches an extra zero.
 * It does not judge the portion (*no judgement about the portion*): it only rejects what cannot be real data.
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

        /** "150", "150,5" or "150.5". `null` if it is not a number or is out of range. */
        fun parseOrNull(text: String): PortionGrams? {
            val trimmed = text.trim()
            if (!NUMBER.matches(trimmed)) return null
            val value = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
            return if (value in MIN..MAX) PortionGrams(value) else null
        }

        /** A figure from the backend (AI proposal, an idea's ingredient) if it meets the rule. */
        fun ofOrNull(value: Double?): PortionGrams? =
            value?.takeIf { it.isFinite() && it in MIN..MAX }?.let(::PortionGrams)
    }
}
