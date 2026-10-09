package pe.edu.upc.healthify.features.intake.infrastructure

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.DomainException
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.GuidelineCode
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.intake.infrastructure.remote.ActiveTargetsService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GuidelineItemDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PlanChangeDto
import pe.edu.upc.healthify.features.intake.infrastructure.repository.ActiveTargetsRepositoryImpl
import pe.edu.upc.healthify.testing.fixedClock
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant

class ActiveTargetsInfrastructureTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }
    private val service = FakeActiveTargetsService()
    private val dao = FakeActiveTargetsCacheDao()
    private val repository = ActiveTargetsRepositoryImpl(service, dao, json, fixedClock)

    private fun dto(
        guidelineItems: List<GuidelineItemDto>? = listOf(GuidelineItemDto(code = "ReduceSalt"), GuidelineItemDto(custom = "Caminar 20 min")),
        restrictions: List<String> = listOf("ShellfishFree"),
        legacyRestrictions: List<String>? = listOf("Sin ají"),
    ) = ActiveTargetsDto(
        patientId = 12,
        planVersion = 3,
        validFrom = "2026-09-04T10:00:00.1234567-05:00",
        energyKcal = 1850.0,
        proteinG = 90.0,
        carbG = 238.0,
        fatG = 60.0,
        guidelines = listOf("ReduceSalt", "Caminar 20 min"),
        restrictions = restrictions,
        refreshedAt = "2026-09-04T15:00:00Z",
        guidelineItems = guidelineItems,
        legacyRestrictions = legacyRestrictions,
        changesFromPrevious = listOf(PlanChangeDto(type = "GuidelineAdded", code = "ReduceSalt"), PlanChangeDto(type = "NoTargetChanges")),
        patientMessage = "  Probemos con estas ideas.  ",
    )

    @Test
    fun `mapper keeps codes for translation and free text as written`() {
        val targets = dto().toDomain()

        assertEquals(Instant.parse("2026-09-04T15:00:00.1234567Z"), targets.validFrom)
        assertEquals(
            listOf(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT), PlanGuideline.Custom("Caminar 20 min")),
            targets.guidelines,
        )
        // Primero los códigos, después las restricciones legadas tal cual.
        assertEquals(
            listOf(DietaryRestriction.Catalog(RestrictionCode.SHELLFISH_FREE), DietaryRestriction.Legacy("Sin ají")),
            targets.restrictions,
        )
        assertEquals(
            listOf(PlanChange.GuidelineAdded(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT)), PlanChange.NoTargetChanges),
            targets.changesFromPrevious,
        )
        assertEquals("Probemos con estas ideas.", targets.patientMessage)
    }

    @Test
    fun `without guideline items (older backend) each plain guideline is a code or a custom text`() {
        val targets = dto(guidelineItems = null).toDomain()

        assertEquals(
            listOf(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT), PlanGuideline.Custom("Caminar 20 min")),
            targets.guidelines,
        )
    }

    @Test
    fun `remote targets are returned and saved on the phone`() = runTest {
        service.response = { dto() }

        val lookup = repository.getActiveTargets(PatientId(12)).getOrThrow()

        assertFalse(lookup.fromCache)
        assertEquals(3, lookup.targets?.planVersion)
        val saved = dao.rows.getValue(12)
        assertEquals(Instant.parse("2026-10-07T13:00:00Z").toEpochMilli(), saved.savedAtEpochMillis)
    }

    @Test
    fun `offline returns the saved copy with the moment it was saved`() = runTest {
        service.response = { dto() }
        repository.getActiveTargets(PatientId(12))
        service.response = { throw IOException("offline") }

        val lookup = repository.getActiveTargets(PatientId(12)).getOrThrow()

        assertTrue(lookup.fromCache)
        assertEquals(Instant.parse("2026-10-07T13:00:00Z"), lookup.savedAt)
        assertEquals(listOf(DietaryRestriction.Catalog(RestrictionCode.SHELLFISH_FREE), DietaryRestriction.Legacy("Sin ají")), lookup.targets?.restrictions)
    }

    @Test
    fun `offline without a saved copy is a network error`() = runTest {
        service.response = { throw IOException("offline") }

        val result = repository.getActiveTargets(PatientId(12))

        assertEquals(DomainError.Network, result.domainErrorOrNull())
    }

    @Test
    fun `404 means no targets and clears the copy`() = runTest {
        service.response = { dto() }
        repository.getActiveTargets(PatientId(12))
        service.response = { throw httpError(404, """{"code":"ActiveTargetsCacheNotFound"}""") }

        val lookup = repository.getActiveTargets(PatientId(12)).getOrThrow()

        assertNull(lookup.targets)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `a resource that breaks the invariants falls back to the copy`() = runTest {
        service.response = { dto() }
        repository.getActiveTargets(PatientId(12))
        service.response = { dto().copy(energyKcal = 0.0) }

        val lookup = repository.getActiveTargets(PatientId(12)).getOrThrow()

        assertTrue(lookup.fromCache)
        assertEquals(1850.0, lookup.targets?.targets?.energyKcal ?: 0.0, 0.0)
    }

    @Test
    fun `a damaged copy is treated as no copy`() = runTest {
        dao.rows[12] = ActiveTargetsCacheEntity(12, "{not json", 0)
        service.response = { throw IOException("offline") }

        val result = repository.getActiveTargets(PatientId(12))

        assertTrue(result.exceptionOrNull() is DomainException)
    }

    private fun httpError(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/problem+json".toMediaType())))
}

private class FakeActiveTargetsService : ActiveTargetsService {
    var response: () -> ActiveTargetsDto = { error("not programmed") }
    override suspend fun getActiveTargets(patientId: Long): ActiveTargetsDto = response()
}

private class FakeActiveTargetsCacheDao : ActiveTargetsCacheDao {
    val rows = mutableMapOf<Long, ActiveTargetsCacheEntity>()
    override suspend fun get(patientId: Long) = rows[patientId]
    override suspend fun upsert(entity: ActiveTargetsCacheEntity) {
        rows[entity.patientId] = entity
    }

    override suspend fun delete(patientId: Long) {
        rows.remove(patientId)
    }
}
