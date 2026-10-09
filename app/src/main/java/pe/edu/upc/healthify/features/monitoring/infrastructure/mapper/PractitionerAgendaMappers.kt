package pe.edu.upc.healthify.features.monitoring.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.RecordReferralRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.RescheduleFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduleFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduledFollowUpDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Una visita con un estado que esta versión no conoce, o sin fecha legible, no se lista (`null`). */
fun ScheduledFollowUpDto.toDomainOrNull(): AgendaVisit? {
    val moment = scheduledFor.toInstantOrNull() ?: return null
    val visitState = FollowUpState.fromCode(state) ?: return null
    return AgendaVisit(
        id = followUpId,
        patientId = PatientId(patientId),
        patientFullName = patientFullName?.trim()?.takeIf(String::isNotEmpty),
        scheduledFor = moment,
        state = visitState,
        preparation = preparation.orEmpty().mapNotNull(PreparationInstruction::of),
        modality = ConsultationModality.fromCode(modality),
    )
}

fun NewFollowUp.toDto(): ScheduleFollowUpRequestDto = ScheduleFollowUpRequestDto(
    patientId = patientId.value,
    scheduledFor = scheduledFor.toIsoOffset(),
    preparation = preparation.toCodes(),
    modality = modality.code,
)

fun rescheduleDto(scheduledFor: Instant, preparation: FollowUpPreparation) =
    RescheduleFollowUpRequestDto(scheduledFor.toIsoOffset(), preparation.toCodes())

fun NewReferral.toDto(): RecordReferralRequestDto =
    RecordReferralRequestDto(patientId.value, specialty.text.trim(), reason.text.trim())

private fun FollowUpPreparation.toCodes(): List<String> = orderedCodes.map { it.code }

/** `DateTimeOffset` en UTC («2026-09-18T15:00:00Z»). */
internal fun Instant.toIsoOffset(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(atOffset(ZoneOffset.UTC))
