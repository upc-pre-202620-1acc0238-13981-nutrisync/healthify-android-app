package pe.edu.upc.healthify.testing

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.GuidelineCode
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PlanVersionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId as IntakePatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId as MonitoringPatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId as NutritionalCarePatientId

/** «Hoy» de los tests: 2026-10-07 a las 13:00 UTC, en UTC. */
val TODAY: LocalDate = LocalDate.of(2026, 10, 7)
val fixedClock: Clock = Clock.fixed(Instant.parse("2026-10-07T13:00:00Z"), ZoneOffset.UTC)

/** Metas vigentes como las de PT4 del Figma (v3, 1 850 kcal, «Sin mariscos»). */
fun activeTargets(
    planVersion: Int = 3,
    validFrom: Instant = Instant.parse("2026-09-04T15:00:00Z"),
    restrictions: List<DietaryRestriction> = listOf(DietaryRestriction.Catalog(RestrictionCode.SHELLFISH_FREE)),
    patientMessage: String? = null,
) = ActiveTargets(
    patientId = IntakePatientId(12),
    planVersion = planVersion,
    validFrom = validFrom,
    targets = DailyTargets(energyKcal = 1850.0, proteinG = 90.0, carbG = 238.0, fatG = 60.0),
    guidelines = listOf(
        PlanGuideline.Catalog(GuidelineCode.PRIORITIZE_VEGETABLES),
        PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT),
    ),
    restrictions = restrictions,
    changesFromPrevious = listOf(
        PlanChange.GuidelineAdded(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT)),
        PlanChange.NoTargetChanges,
    ),
    patientMessage = patientMessage,
)

class FakeActiveTargetsRepository : ActiveTargetsRepository {
    var result: Result<ActiveTargetsLookup> = Result.success(ActiveTargetsLookup(activeTargets()))
    var calls = 0

    override suspend fun getActiveTargets(patientId: IntakePatientId): Result<ActiveTargetsLookup> {
        calls++
        return result
    }
}

class FakePatientMonitoringRepository : PatientMonitoringRepository {
    var progressResult: Result<DailyProgress> =
        Result.success(DailyProgress(TODAY, ComplianceOutcome.MET, observedEnergyKcal = 1260.0))
    var summaryResult: Result<ComplianceSummary> = Result.success(summary(loggedDays = 2))
    var consistencyResult: Result<ConsistencyIndex?> = Result.success(null)
    var acknowledgeResult: Result<Unit> = Result.success(Unit)
    var nextFollowUpResult: Result<NextFollowUp?> = Result.success(null)

    val summaryRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var acknowledgements = 0

    override suspend fun getDailyProgress(patientId: MonitoringPatientId, date: LocalDate) = progressResult

    override suspend fun getComplianceSummary(
        patientId: MonitoringPatientId,
        from: LocalDate,
        to: LocalDate,
    ): Result<ComplianceSummary> {
        summaryRanges += from to to
        return summaryResult
    }

    override suspend fun getConsistencyIndex(patientId: MonitoringPatientId) = consistencyResult

    override suspend fun acknowledgeConsistencyPrompt(patientId: MonitoringPatientId): Result<Unit> {
        acknowledgements++
        return acknowledgeResult
    }

    override suspend fun getNextFollowUp(patientId: MonitoringPatientId) = nextFollowUpResult
}

fun summary(loggedDays: Int, totalDays: Int = 3) = ComplianceSummary(
    metDays = loggedDays,
    exceededDays = 0,
    shortDays = 0,
    unloggedDays = totalDays - loggedDays,
    loggedDays = loggedDays,
    totalDays = totalDays,
)

class FakePlanVersionRepository : PlanVersionRepository {
    var result: Result<List<PlanVersion>> = Result.success(emptyList())

    override suspend fun getPlanVersions(patientId: NutritionalCarePatientId) = result
}
