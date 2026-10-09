package pe.edu.upc.healthify.features.foodcatalog

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.foodcatalog.application.usecase.SearchFoodsUseCase
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.FoodSearchResult
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodDao
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodEntity
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.mapper.normalizeFoodName
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.ReferenceFoodService
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.CreateLocalOverrideRequestDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.dto.ReferenceFoodDto
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.repository.ReferenceFoodRepositoryImpl
import pe.edu.upc.healthify.testing.FakeReferenceFoodRepository
import pe.edu.upc.healthify.testing.failureOf
import pe.edu.upc.healthify.testing.fixedClock
import pe.edu.upc.healthify.testing.referenceFood
import java.io.IOException
import java.time.Duration

class FoodCatalogTest {

    @Test
    fun `search terms need 2 characters and are trimmed`() {
        assertNull(FoodSearchTerm.of(" q "))
        assertEquals("arroz con pollo", FoodSearchTerm.of("  arroz   con pollo ")!!.value)
    }

    @Test
    fun `merge keeps the local order and adds only what the phone did not have`() {
        val merged = FoodSearchResult.merge(
            local = listOf(referenceFood(1, "Quinua"), referenceFood(2, "Quinua con leche")),
            remote = listOf(referenceFood(2, "Quinua con leche"), referenceFood(3, "Ensalada de quinua")),
        )
        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id.value })
    }

    @Test
    fun `the use case emits local results first, then the merged list`() = runTest {
        val repository = FakeReferenceFoodRepository().apply {
            local = listOf(referenceFood(1, "Quinua cocida"))
            remote = Result.success(listOf(referenceFood(5, "Quinua roja")))
        }

        SearchFoodsUseCase(repository)(FoodSearchTerm.of("quinua")!!).test {
            val first = awaitItem()
            assertTrue(first.serverPending)
            assertEquals(listOf(1L), first.foods.map { it.id.value })
            val second = awaitItem()
            assertFalse(second.serverPending)
            assertEquals(listOf(1L, 5L), second.foods.map { it.id.value })
            awaitComplete()
        }
    }

    @Test
    fun `offline the second emission is local only`() = runTest {
        val repository = FakeReferenceFoodRepository().apply {
            local = listOf(referenceFood(1, "Quinua cocida"))
            remote = failureOf(DomainError.Network)
        }

        SearchFoodsUseCase(repository)(FoodSearchTerm.of("quinua")!!).test {
            awaitItem()
            assertTrue(awaitItem().localOnly)
            awaitComplete()
        }
    }

    @Test
    fun `names are normalized without accents nor case`() {
        assertEquals("quinua cocida", normalizeFoodName("  Quínua   COCIDA "))
    }

    @Test
    fun `the phone catalog ranks prefix matches and local overrides first`() = runTest {
        val dao = FakeLocalFoodDao()
        val service = FakeReferenceFoodService(
            listOf(
                dto(1, "Ensalada de quinua"),
                dto(2, "Quinua con leche"),
                dto(3, "Quínua cocida", override = true),
                dto(4, "Arroz blanco"),
            ),
        )
        val repository = ReferenceFoodRepositoryImpl(service, dao, fixedClock)
        repository.refreshLocalCatalog(patientId = 12)

        val found = repository.searchLocal(FoodSearchTerm.of("quinua")!!)

        assertEquals(listOf(3L, 2L, 1L), found.map { it.id.value })
    }

    @Test
    fun `the phone catalog is refreshed at most once a day and kept when offline`() = runTest {
        val dao = FakeLocalFoodDao()
        val service = FakeReferenceFoodService(listOf(dto(1, "Arroz")))
        val repository = ReferenceFoodRepositoryImpl(service, dao, fixedClock)

        repository.refreshLocalCatalog(12)
        repository.refreshLocalCatalog(12)
        assertEquals(1, service.catalogCalls)

        service.fail = true
        val forced = repository.refreshLocalCatalog(12, force = true)
        assertEquals(DomainError.Network, forced.domainErrorOrNull())
        assertEquals(1, dao.getAll().size)
        assertTrue(Duration.ofMillis(fixedClock.millis() - dao.savedAt()!!).isZero)
    }

    private fun dto(id: Long, name: String, override: Boolean = false) = ReferenceFoodDto(id, name, 120.0, 4.0, 20.0, 2.0, override)

    private class FakeReferenceFoodService(private val catalog: List<ReferenceFoodDto>) : ReferenceFoodService {
        var fail = false
        var catalogCalls = 0

        override suspend fun search(query: String, max: Int): List<ReferenceFoodDto> {
            if (fail) throw IOException("offline")
            return catalog.filter { normalizeFoodName(it.localName).contains(normalizeFoodName(query)) }
        }

        override suspend fun getLocalFoodCatalog(patientId: Long): List<ReferenceFoodDto> {
            if (fail) throw IOException("offline")
            catalogCalls++
            return catalog
        }

        val createdBodies = mutableListOf<CreateLocalOverrideRequestDto>()

        override suspend fun createLocalOverride(body: CreateLocalOverrideRequestDto): ReferenceFoodDto {
            if (fail) throw IOException("offline")
            createdBodies += body
            return ReferenceFoodDto(77, body.localName, body.energyKcalPer100g, body.proteinGPer100g, body.carbGPer100g, body.fatGPer100g, true)
        }
    }

    @Test
    fun `PR15 browses the server catalog with an empty query and PR15_1 creates a local override`() = runTest {
        val service = FakeReferenceFoodService(listOf(dto(1, "Papa amarilla"), dto(2, "Cuy al horno", override = true)))
        val repository = ReferenceFoodRepositoryImpl(service, FakeLocalFoodDao(), fixedClock)

        assertEquals(2, repository.browseCatalog(term = null).getOrThrow().size)
        val created = repository.createLocalFood(NewLocalFood("Chaufa de cuy", 180.0, 12.0, 20.0, 6.0)).getOrThrow()

        assertTrue(created.isLocalOverride)
        assertEquals(CreateLocalOverrideRequestDto("Chaufa de cuy", 180.0, 12.0, 20.0, 6.0), service.createdBodies.single())
        service.fail = true
        assertEquals(DomainError.Network, repository.createLocalFood(NewLocalFood("Cuy", 1.0, 1.0, 1.0, 1.0)).domainErrorOrNull())
    }

    private class FakeLocalFoodDao : LocalFoodDao {
        private var rows = listOf<LocalFoodEntity>()

        override suspend fun getAll(): List<LocalFoodEntity> = rows.sortedBy { it.position }

        override suspend fun savedAt(): Long? = rows.minOfOrNull { it.savedAtEpochMillis }

        override suspend fun insertAll(foods: List<LocalFoodEntity>) {
            rows = rows + foods
        }

        override suspend fun deleteAll() {
            rows = emptyList()
        }
    }
}
