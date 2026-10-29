package pe.edu.upc.healthify.features.foodcatalog.domain.entity

import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm

/**
 * Result of a local-first search: first the catalog saved on the phone, then what the server
 * added.
 *
 * @param serverPending the server response is still pending (the list may grow).
 * @param localOnly the server did not answer (offline): only results from the saved catalog.
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
        /** Maximum number of results shown (the same `max` requested from the server). */
        const val MAX_RESULTS = 25

        /** Local results first (already sorted), then the server ones the phone did not have. */
        fun merge(local: List<ReferenceFood>, remote: List<ReferenceFood>): List<ReferenceFood> {
            val seen = local.map { it.id }.toMutableSet()
            return (local + remote.filter { seen.add(it.id) }).take(MAX_RESULTS)
        }
    }
}
