package pe.edu.upc.healthify.features.foodcatalog.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.FoodSearchResult
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodDao
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodEntity
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.normalizeFoodName
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.toDomainSkippingInvalid
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.toEntity
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.ReferenceFoodService
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Búsqueda local-first: la copia de Room responde sin servidor; el servidor completa. La copia se renueva como
 * mucho una vez al día (o al pedirlo), siempre entera.
 */
class ReferenceFoodRepositoryImpl @Inject constructor(
    private val service: ReferenceFoodService,
    private val dao: LocalFoodDao,
    private val clock: Clock,
) : ReferenceFoodRepository {

    override suspend fun searchLocal(term: FoodSearchTerm): List<ReferenceFood> {
        val query = normalizeFoodName(term.value)
        val words = query.split(' ')
        return dao.getAll()
            .filter { food -> words.all { it in food.normalizedName } }
            .sortedWith(compareBy<LocalFoodEntity>({ rank(it.normalizedName, query) }, { !it.isLocalOverride }, { it.position }))
            .take(FoodSearchResult.MAX_RESULTS)
            .map { it.toDomain() }
    }

    override suspend fun searchRemote(term: FoodSearchTerm): Result<List<ReferenceFood>> =
        apiCall { service.search(term.value, FoodSearchResult.MAX_RESULTS) }.map { it.toDomainSkippingInvalid() }

    override suspend fun browseCatalog(term: FoodSearchTerm?): Result<List<ReferenceFood>> =
        apiCall { service.search(term?.value.orEmpty(), FoodSearchResult.MAX_RESULTS) }.map { it.toDomainSkippingInvalid() }

    override suspend fun createLocalFood(food: NewLocalFood): Result<ReferenceFood> =
        apiCall { service.createLocalOverride(food.toDto()) }.fold(
            onSuccess = { dto ->
                try {
                    Result.success(dto.toDomain())
                } catch (_: IllegalArgumentException) {
                    domainFailure(DomainError.Unexpected("MALFORMED_RESPONSE"))
                }
            },
            onFailure = { Result.failure(it) },
        )

    override suspend fun refreshLocalCatalog(patientId: Long, force: Boolean): Result<Unit> {
        val now = clock.millis()
        val savedAt = dao.savedAt()
        if (!force && savedAt != null && now - savedAt < MAX_AGE.toMillis()) return Result.success(Unit)
        return apiCall { service.getLocalFoodCatalog(patientId) }.map { dtos ->
            val foods = dtos.toDomainSkippingInvalid().distinctBy { it.id }
            // Una respuesta vacía no borra la copia: sin catálogo no se podría registrar nada offline.
            if (foods.isNotEmpty()) {
                dao.replaceAll(foods.mapIndexed { index, food -> food.toEntity(position = index, savedAtEpochMillis = now) })
            }
        }
    }

    /** 0: empieza igual · 1: una palabra empieza igual · 2: lo contiene en otra parte. */
    private fun rank(name: String, query: String): Int = when {
        name.startsWith(query) -> 0
        name.split(' ').any { it.startsWith(query) } -> 1
        else -> 2
    }

    private companion object {
        val MAX_AGE: Duration = Duration.ofHours(24)
    }
}
