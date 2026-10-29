package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyState
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ComplianceDayDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ComplianceSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsistencyIndexDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientFollowUpDto
import java.time.LocalDate

/**
 * The days for [date] (one per window; more than one only if the patient changed nutritionist that day). The last
 * evaluated one wins. With no days, or an unknown outcome, the day stays "no log": a verdict is never made up.
 */
fun List<DailyComplianceDto>.toDailyProgress(date: LocalDate): DailyProgress {
    val day = lastOrNull { it.date.toLocalDateOrNull() == date } ?: return DailyProgress.unlogged(date)
    val outcome = ComplianceOutcome.fromCode(day.outcome) ?: return DailyProgress.unlogged(date)
    return DailyProgress(date, outcome, observedEnergyKcal = day.observedEnergyKcal.coerceAtLeast(0.0))
}

/** A day with an unreadable date or unknown outcome is skipped: it stays "no log", never made up. */
fun ComplianceDayDto.toDomainOrNull(): ComplianceDay? {
    val day = date.toLocalDateOrNull() ?: return null
    val result = ComplianceOutcome.fromCode(outcome) ?: return null
    return ComplianceDay(day, result)
}

fun ComplianceSummaryDto.toDomain(): ComplianceSummary = ComplianceSummary(
    metDays = metDays,
    exceededDays = exceededDays,
    shortDays = shortDays,
    unloggedDays = unloggedDays,
    loggedDays = loggedDays,
    totalDays = totalDays,
)

fun ConsistencyIndexDto.toDomain(): ConsistencyIndex = ConsistencyIndex(
    state = ConsistencyState.fromCode(state),
    patientPromptPending = patientPromptPending,
)

/** @throws IllegalArgumentException if the date cannot be read or the id is invalid. */
fun PatientFollowUpDto.toDomain(): NextFollowUp = nextFollowUpOf(
    followUpId = followUpId,
    scheduledFor = scheduledFor,
    modality = modality,
    preparation = preparation,
    scheduledAt = scheduledAt,
    practitionerFullName = practitionerFullName,
)

/** Shared shape of `PatientFollowUpResource` and `UpcomingConsultationResource`. */
internal fun nextFollowUpOf(
    followUpId: Long,
    scheduledFor: String,
    modality: String?,
    preparation: List<String>,
    scheduledAt: String?,
    practitionerFullName: String?,
): NextFollowUp = NextFollowUp(
    id = FollowUpId(followUpId),
    scheduledFor = requireNotNull(scheduledFor.toInstantOrNull()) { "Unreadable scheduledFor" },
    modality = ConsultationModality.fromCode(modality),
    preparation = preparation.mapNotNull(PreparationInstruction::of).distinct(),
    scheduledAt = scheduledAt?.toInstantOrNull(),
    practitionerFullName = practitionerFullName?.trim()?.takeIf { it.isNotEmpty() },
)
