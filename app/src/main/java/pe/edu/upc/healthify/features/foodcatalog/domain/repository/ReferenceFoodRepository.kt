package pe.edu.upc.healthify.features.foodcatalog.domain.repository

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm

/** Food catalog: the phone's copy (≤ 500, offline) and the server search. */
interface ReferenceFoodRepository {

    /** Searches the phone's copy: the nutritionist's local foods first, then those with the same prefix. */
    suspend fun searchLocal(term: FoodSearchTerm): List<ReferenceFood>

    /** `GET /reference-foods?query=&max=25` (anonymous and local-first on the server). */
    suspend fun searchRemote(term: FoodSearchTerm): Result<List<ReferenceFood>>

    /**
     * Refreshes the phone's copy with `GET /patients/{patientId}/local-food-catalog` if it is older than a day (or
     * missing). Offline it changes nothing and fails with `DomainError.Network`.
     */
    suspend fun refreshLocalCatalog(patientId: Long, force: Boolean = false): Result<Unit>

    /**
     * `GET /reference-foods?query=&max=25` without the phone's copy (catalog management is not offline).
     * [term] `null` = no text: the server lists its local catalog (local foods first).
     */
    suspend fun browseCatalog(term: FoodSearchTerm?): Result<List<ReferenceFood>>

    /**
     * `POST /reference-foods/local-overrides` (nutritionist only). `400 LocalNameAndNutrientsRequired` and
     * `409 DuplicatedLocalOverride` if a local food with that name already exists.
     */
    suspend fun createLocalFood(food: NewLocalFood): Result<ReferenceFood>
}
