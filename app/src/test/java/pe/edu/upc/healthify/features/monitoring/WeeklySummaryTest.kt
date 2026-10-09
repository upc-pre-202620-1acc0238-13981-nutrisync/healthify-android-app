package pe.edu.upc.healthify.features.monitoring

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.GetLatestWeeklySummaryUseCase
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryFacts
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.WeeklySummaryService
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.WeeklySummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.WeeklySummaryFactsDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.WeeklySummaryRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.presentation.screen.weekRangeTextParts
import pe.edu.upc.healthify.features.monitoring.presentation.viewmodel.WeeklySummaryViewModel
import pe.edu.upc.healthify.testing.FakeConnectivityObserver
import pe.edu.upc.healthify.testing.FakeSessionRepository
import pe.edu.upc.healthify.testing.FakeWeeklySummaryRepository
import pe.edu.upc.healthify.testing.MainDispatcherRule
import pe.edu.upc.healthify.testing.failureOf
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.util.Locale

/** IA-2 · resumen semanal (PT13 card «Tu semana», PT13.2 y PT13.2.V). */
class WeeklySummaryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val service = FakeWeeklySummaryService()
    private val repository = WeeklySummaryRepositoryImpl(service)

    @Test
    fun `a summary is read with its bullets as written by the AI`() = runTest {
        val availability = repository.getLatest(PATIENT).getOrThrow()

        val summary = (availability as WeeklySummaryAvailability.Ready).summary
        assertEquals("Cumpliste tus metas 5 de 7 días.", summary.headline)
        assertEquals(listOf("Registraste tus comidas 6 de 7 días."), summary.wentWell)
        assertEquals(LocalDate.of(2026, 9, 8), summary.weekStart)
        assertEquals(-0.3, summary.facts.weightChangeKg!!, 0.0)
    }

    @Test
    fun `404 NotEnoughData is PT13_2_V, not an error`() = runTest {
        service.failWith = http(404, "NotEnoughData")

        assertEquals(WeeklySummaryAvailability.NotYet, repository.getLatest(PATIENT).getOrThrow())
    }

    @Test
    fun `403 or the AI switched off on the server hide the card`() = runTest {
        service.failWith = http(403, "AiConsentRequired")
        assertEquals(WeeklySummaryAvailability.Off, repository.getLatest(PATIENT).getOrThrow())

        service.failWith = HttpException(Response.error<Any>(403, "".toResponseBody()))
        assertEquals(WeeklySummaryAvailability.Off, repository.getLatest(PATIENT).getOrThrow())

        service.failWith = http(503, "AiFeatureDisabled")
        assertEquals(WeeklySummaryAvailability.Off, repository.getLatest(PATIENT).getOrThrow())
    }

    @Test
    fun `without network or with a provider outage the error is returned`() = runTest {
        service.failWith = IOException("offline")
        assertEquals(DomainError.Network, repository.getLatest(PATIENT).domainErrorOrNull())

        service.failWith = http(503, "AiProviderUnavailable")
        assertTrue(repository.getLatest(PATIENT).domainErrorOrNull() is DomainError.Unexpected)
    }

    @Test
    fun `a summary with a blank headline or impossible figures is not shown`() {
        assertNull(dto().copy(headline = " ").toDomainOrNull())
        assertNull(dto().copy(facts = WeeklySummaryFactsDto(metDays = 6, totalDays = 7, loggedDays = 4)).toDomainOrNull())
        assertNull(dto().copy(weekEnd = "2026-09-01").toDomainOrNull())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a day within the targets is always a logged day`() {
        WeeklySummaryFacts(metDays = 5, totalDays = 7, loggedDays = 4, unloggedDays = 3, weightChangeKg = null)
    }

    @Test
    fun `the week reads naturally in each language`() {
        val start = LocalDate.of(2026, 9, 8)
        val end = LocalDate.of(2026, 9, 14)
        assertEquals("8" to "14 de septiembre", weekRangeTextParts(start, end, Locale.forLanguageTag("es")))
        assertEquals("September 8" to "14", weekRangeTextParts(start, end, Locale.ENGLISH))
        assertEquals(
            "29 de septiembre" to "5 de octubre",
            weekRangeTextParts(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 10, 5), Locale.forLanguageTag("es")),
        )
    }

    @Test
    fun `PT13_2 shows the summary, PT13_2_V the empty state and offline an offline state`() {
        val fake = FakeWeeklySummaryRepository()
        val connectivity = FakeConnectivityObserver()
        val viewModel = WeeklySummaryViewModel(
            ObserveCurrentUserUseCase(FakeSessionRepository()),
            GetLatestWeeklySummaryUseCase(fake),
            connectivity,
        )
        assertEquals("Cumpliste tus metas 5 de 7 días.", viewModel.state.value.summary?.headline)
        assertFalse(viewModel.state.value.isLoading)

        fake.result = Result.success(WeeklySummaryAvailability.NotYet)
        viewModel.onRetry()
        assertTrue(viewModel.state.value.notYet)
        assertNull(viewModel.state.value.summary)

        fake.result = failureOf(DomainError.Network)
        viewModel.onRetry()
        assertTrue(viewModel.state.value.loadFailed)
        assertTrue(viewModel.state.value.isOffline)
    }

    private fun http(status: Int, code: String) = HttpException(
        Response.error<Any>(
            status,
            """{"title":"x","status":$status,"code":"$code"}""".toResponseBody(),
        ),
    )

    private fun dto() = WeeklySummaryDto(
        weekStart = "2026-09-08",
        weekEnd = "2026-09-14",
        headline = "Cumpliste tus metas 5 de 7 días.",
        wentWell = listOf("Registraste tus comidas 6 de 7 días.", " "),
        watchOut = listOf("Los fines de semana registras menos."),
        facts = WeeklySummaryFactsDto(metDays = 5, totalDays = 7, loggedDays = 6, unloggedDays = 1, weightChangeKg = -0.3),
        language = "es",
        generatedAt = "2026-09-15T06:00:00-05:00",
    )

    private inner class FakeWeeklySummaryService : WeeklySummaryService {
        var failWith: Exception? = null

        override suspend fun getLatest(patientId: Long): WeeklySummaryDto {
            failWith?.let { throw it }
            return dto()
        }
    }

    private companion object {
        val PATIENT = PatientId(12)
    }
}
