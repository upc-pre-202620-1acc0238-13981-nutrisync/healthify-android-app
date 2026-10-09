package pe.edu.upc.healthify.features.foodcatalog.domain.entity

import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm

/**
 * Resultado de una búsqueda local-first (PT9, F14b): primero el catálogo guardado en el teléfono, después lo que
 * agregó el servidor.
 *
 * @param serverPending todavía se espera la respuesta del servidor (la lista puede crecer).
 * @param localOnly el servidor no respondió (sin conexión): solo hay resultados del catálogo guardado (PT10 offline).
 */
data class FoodSearchResult(
    val term: FoodSearchTerm,
    val foods: List<ReferenceFood>,
    val serverPending: Boolean,
    val localOnly: Boolean,
) {
    init {
        require(!(serverPending && localOnly)) { "A search cannot be pending and finished offline at once" }
        require(foods.map { it.id }.toSet().size == foods.size) { "A food appears only once" }
    }

    val isEmpty: Boolean get() = foods.isEmpty()

    companion object {
        /** Máximo de resultados que se muestran (el mismo `max` que se pide al servidor). */
        const val MAX_RESULTS = 25

        /** Los locales primero (ya ordenados), después los del servidor que el teléfono no tenía. */
        fun merge(local: List<ReferenceFood>, remote: List<ReferenceFood>): List<ReferenceFood> {
            val seen = local.map { it.id }.toMutableSet()
            return (local + remote.filter { seen.add(it.id) }).take(MAX_RESULTS)
        }
    }
}
