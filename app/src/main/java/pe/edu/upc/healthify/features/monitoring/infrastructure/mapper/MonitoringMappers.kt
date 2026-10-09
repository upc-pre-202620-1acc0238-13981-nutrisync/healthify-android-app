package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyState
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ComplianceSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsistencyIndexDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientFollowUpDto
import java.time.LocalDate

/**
 * Los días de [date] (uno por ventana; más de uno solo si el paciente cambió de nutricionista ese día, CR-1). Manda
 * el último evaluado. Sin días, o con un resultado desconocido, el día queda «sin registro»: nunca se inventa un
 * veredicto.
 */
fun List<DailyComplianceDto>.toDailyProgress(date: LocalDate): DailyProgress {
    val day = lastOrNull { it.date.toLocalDateOrNull() == date } ?: return DailyProgress.unlogged(date)
    val outcome = ComplianceOutcome.fromCode(day.outcome) ?: return DailyProgress.unlogged(date)
    return DailyProgress(date, outcome, observedEnergyKcal = day.observedEnergyKcal.coerceAtLeast(0.0))
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

/** @throws IllegalArgumentException si la fecha no se puede leer o el id no es válido. */
fun PatientFollowUpDto.toDomain(): NextFollowUp = nextFollowUpOf(
    followUpId = followUpId,
    scheduledFor = scheduledFor,
    modality = modality,
    preparation = preparation,
    scheduledAt = scheduledAt,
    practitionerFullName = practitionerFullName,
)

/** Forma común de `PatientFollowUpResource` (MA-3) y `UpcomingConsultationResource` (RM-5). */
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
