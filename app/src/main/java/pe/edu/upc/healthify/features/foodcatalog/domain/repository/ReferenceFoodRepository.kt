package pe.edu.upc.healthify.features.foodcatalog.domain.repository

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm

/** Catálogo de alimentos: la copia del teléfono (≤ 500, offline) y la búsqueda del servidor (§5.4, F14b). */
interface ReferenceFoodRepository {

    /** Busca en la copia del teléfono: los locales del nutricionista primero, después los que empiezan igual. */
    suspend fun searchLocal(term: FoodSearchTerm): List<ReferenceFood>

    /** `GET /reference-foods?query=&max=25` (anónimo y local-first en el servidor). */
    suspend fun searchRemote(term: FoodSearchTerm): Result<List<ReferenceFood>>

    /**
     * Renueva la copia del teléfono con `GET /patients/{patientId}/local-food-catalog` si tiene más de un día (o no
     * hay). Sin conexión no cambia nada y falla con `DomainError.Network`.
     */
    suspend fun refreshLocalCatalog(patientId: Long, force: Boolean = false): Result<Unit>

    /**
     * PR15 · `GET /reference-foods?query=&max=25` sin la copia del teléfono (la gestión del catálogo no es offline).
     * [term] `null` = sin texto: el servidor lista su catálogo local (los alimentos locales primero).
     */
    suspend fun browseCatalog(term: FoodSearchTerm?): Result<List<ReferenceFood>>

    /**
     * PR15.1 · `POST /reference-foods/local-overrides` (solo nutricionista). `400 LocalNameAndNutrientsRequired` y
     * `409 DuplicatedLocalOverride` si ya hay un alimento local con ese nombre.
     */
    suspend fun createLocalFood(food: NewLocalFood): Result<ReferenceFood>
}
