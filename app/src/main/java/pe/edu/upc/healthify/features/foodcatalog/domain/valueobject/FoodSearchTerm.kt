package pe.edu.upc.healthify.features.foodcatalog.domain.valueobject

/**
 * Lo que el paciente escribió en «Buscar alimento» (PT9), sin espacios sobrantes.
 *
 * DECISIÓN PT9: se busca desde 2 caracteres («qu» ya filtra el catálogo del teléfono); el servidor completa con
 * proveedores externos recién desde 3 (F14b), así que con 2 la respuesta es la del catálogo local del servidor.
 */
@JvmInline
value class FoodSearchTerm private constructor(val value: String) {

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 80

        /** `null` si es demasiado corto para buscar (la lista queda vacía, sin «No lo encontramos»). */
        fun of(raw: String): FoodSearchTerm? {
            val trimmed = raw.trim().replace(WHITESPACE, " ")
            return if (trimmed.length < MIN_LENGTH) null else FoodSearchTerm(trimmed.take(MAX_LENGTH))
        }

        private val WHITESPACE = Regex("\\s+")
    }
}
