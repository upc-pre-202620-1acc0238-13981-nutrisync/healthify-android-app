package pe.edu.upc.healthify.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrendPoint
import pe.edu.upc.healthify.features.intake.domain.repository.SelfWeighInRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryFacts
import pe.edu.upc.healthify.features.monitoring.domain.repository.WeeklySummaryRepository
import java.time.Instant
import java.time.LocalDate
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId as MonitoringPatientId

/** Tendencia de [days] puntos, uno cada 2 días hasta [TODAY], dentro de un rango de 4 semanas. */
fun weightTrend(
    days: Int = 6,
    excluded: Int = 0,
    change: Double? = 0.6,
    fromCache: Boolean = false,
): WeightTrend {
    val from = TODAY.minusWeeks(4)
    return WeightTrend(
        points = List(days) { index -> WeightTrendPoint(TODAY.minusDays(2L * (days - 1 - index)), 68.0 + index * 0.1) },
        windowSize = 7,
        lastRecalculatedAt = Instant.parse("2026-10-07T12:00:00Z"),
        excludedReadingsCount = excluded,
        changeKgOverRange = change,
        slopeKgPerWeek = change?.div(4),
        rangeFrom = from,
        rangeTo = TODAY,
        fromCache = fromCache,
        savedAt = if (fromCache) Instant.parse("2026-10-06T13:00:00Z") else null,
    )
}

class FakeSelfWeighInRepository : SelfWeighInRepository {
    var trendResult: Result<WeightTrend?> = Result.success(weightTrend())
    var recordResult: Result<SelfWeighInOutcome> = Result.success(SelfWeighInOutcome.RECORDED)
    val recorded = mutableListOf<NewSelfWeighIn>()
    var trendRequests = 0
    val pending = MutableStateFlow<List<PendingSelfWeighIn>>(emptyList())
    private val notice = MutableStateFlow<SelfWeighInOutcome?>(null)

    override suspend fun record(patientId: PatientId, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome> {
        recorded += weighIn
        recordResult.getOrNull()?.let { notice.value = it }
        return recordResult
    }

    override fun observePending(patientId: PatientId): Flow<List<PendingSelfWeighIn>> = pending

    /** `tryEmit(Unit)` simula que el backend aceptó autopesajes de la cola. */
    val synced = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val syncedWeighIns: Flow<Unit> = synced

    override suspend fun getWeightTrend(patientId: PatientId, weeks: Int): Result<WeightTrend?> {
        trendRequests++
        return trendResult
    }

    override val savedNotice: Flow<SelfWeighInOutcome?> = notice

    override fun consumeSavedNotice() {
        notice.value = null
    }

    fun publishNotice(outcome: SelfWeighInOutcome) {
        notice.value = outcome
    }
}

fun weeklySummary(headline: String = "Cumpliste tus metas 5 de 7 días.") = WeeklySummary(
    weekStart = LocalDate.of(2026, 9, 28),
    weekEnd = LocalDate.of(2026, 10, 4),
    headline = headline,
    wentWell = listOf("Registraste tus comidas 6 de 7 días."),
    watchOut = listOf("Los fines de semana registras menos."),
    facts = WeeklySummaryFacts(metDays = 5, totalDays = 7, loggedDays = 6, unloggedDays = 1, weightChangeKg = -0.3),
    generatedAt = Instant.parse("2026-10-05T11:00:00Z"),
)

class FakeWeeklySummaryRepository(
    var result: Result<WeeklySummaryAvailability> = Result.success(WeeklySummaryAvailability.Ready(weeklySummary())),
) : WeeklySummaryRepository {
    override suspend fun getLatest(patientId: MonitoringPatientId): Result<WeeklySummaryAvailability> = result
}
