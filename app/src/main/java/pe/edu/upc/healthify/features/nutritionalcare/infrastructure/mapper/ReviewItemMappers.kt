package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAdjustmentProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalStatus
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ResolutionNote
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewEvidence
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.AcceptPlanProposalRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanAdjustmentProposalDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ResolveReviewItemRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ReviewItemDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ReviewItemEvidenceDto
import kotlin.math.abs

/**
 * `ReviewItemResource` → [ReviewItem]. La evidencia se arma desde `evidenceData` según el `signalType`; si le falta un
 * campo obligatorio (o es un ítem anterior a X-2) queda `null` y la UI muestra la frase legada `evidence`.
 */
fun ReviewItemDto.toDomain(): ReviewItem {
    val type = SignalType.of(signalType)
    return ReviewItem(
        id = ReviewItemId(reviewItemId),
        patientId = PatientId(patientId),
        patientFullName = patientFullName?.trim()?.takeIf(String::isNotEmpty),
        signalType = type,
        legacyEvidence = evidence,
        evidence = evidenceData?.toEvidence(type),
        isOpen = ReviewItemState.fromCode(state) != ReviewItemState.RESOLVED,
        resolvedWithAdjustment = resolvedWithAdjustment,
        resolutionNote = resolutionNote(),
        createdAt = createdAt?.toInstantOrNull(),
        resolvedAt = resolvedAt?.toInstantOrNull(),
        hasPlanProposal = hasPlanProposal,
    )
}

/** La evidencia de [type]; `null` si faltan sus campos (se cae a la frase legada, nunca a una frase incompleta). */
internal fun ReviewItemEvidenceDto.toEvidence(type: SignalType): ReviewEvidence? = try {
    when (type) {
        SignalType.SustainedDeviation -> {
            val percent = averagePercentFromTarget
            val deviated = deviatedDays
            val logged = loggedDaysConsidered
            // El sentido viene en `direction`; si falta, lo da el signo del porcentaje.
            val sense = DeviationDirection.fromCode(direction)
                ?: percent?.let { if (it < 0) DeviationDirection.BELOW else DeviationDirection.ABOVE }
            if (percent == null || deviated == null || logged == null || sense == null) {
                null
            } else {
                ReviewEvidence.SustainedDeviation(
                    averagePercentFromTarget = abs(percent),
                    deviatedDays = deviated,
                    loggedDaysConsidered = logged,
                    direction = sense,
                    averageEnergyKcalFromTarget = averageEnergyKcalFromTarget?.let(::abs),
                )
            }
        }
        SignalType.ScheduledRecheck -> {
            val date = adjustedOn?.toLocalDateOrNull()
            val version = adjustedPlanVersion
            if (date == null || version == null) {
                null
            } else {
                val bothCounts = deviatedDays != null && loggedDaysConsidered != null
                ReviewEvidence.ScheduledRecheck(
                    adjustedOn = date,
                    adjustedPlanVersion = version,
                    deviatedDays = deviatedDays.takeIf { bothCounts },
                    loggedDaysConsidered = loggedDaysConsidered.takeIf { bothCounts },
                )
            }
        }
        SignalType.ConsistencyEscalation -> consistencyKgPerWeek?.let { index ->
            ReviewEvidence.ConsistencyEscalation(
                kgPerWeek = abs(index),
                alertSinceOn = alertSinceOn?.toLocalDateOrNull(),
                weeksInAlert = weeksInAlert,
                shownToPatientOn = shownToPatientOn?.toLocalDateOrNull(),
            )
        }
        is SignalType.Custom -> null
    }
} catch (_: IllegalArgumentException) {
    // Números que rompen una invariante (p. ej. más días desviados que registrados): mejor la frase legada.
    null
}

private fun ReviewItemDto.resolutionNote(): ResolutionNote? {
    val version = resolutionNoteData?.planVersion
    val text = resolutionNote?.trim()?.takeIf(String::isNotEmpty)
    return when (resolutionNoteCode?.trim()) {
        "PlanAssignedAsIs" -> version?.let(ResolutionNote::PlanAssignedAsIs) ?: text?.let(ResolutionNote::Custom)
        "PlanAssignedWithEdits" -> version?.let(ResolutionNote::PlanAssignedWithEdits) ?: text?.let(ResolutionNote::Custom)
        "ProposalDiscarded" -> ResolutionNote.ProposalDiscarded
        // `Custom`, un código desconocido o ninguno: el texto tal cual (X-2).
        else -> text?.let(ResolutionNote::Custom)
    }
}

fun ReviewResolution.toDto(): ResolveReviewItemRequestDto = ResolveReviewItemRequestDto(resolvedWithAdjustment, note)

/** Una indicación que esta versión no conoce se omite (la IA solo puede devolver códigos del catálogo). */
fun PlanAdjustmentProposalDto.toDomain(): PlanAdjustmentProposal = PlanAdjustmentProposal(
    reviewItemId = ReviewItemId(reviewItemId),
    title = title.trim(),
    currentEnergyKcal = currentEnergyKcal,
    proposed = Targets(proposedEnergyKcal, proposedProteinG, proposedCarbG, proposedFatG),
    addedGuidelines = addedGuidelines.mapNotNull(PlanGuidelineCode::fromCode),
    removedGuidelines = removedGuidelines.mapNotNull(PlanGuidelineCode::fromCode),
    patientMessage = patientMessage.trim(),
    recheckAfterDays = recheckAfterDays,
    rationale = rationale.trim(),
    generatedAt = requireNotNull(generatedAt.toInstantOrNull()) { "generatedAt is not a date" },
    status = PlanProposalStatus.fromCode(status),
)

fun PlanAcceptance.toDto(): AcceptPlanProposalRequestDto = when (this) {
    PlanAcceptance.AsIs -> AcceptPlanProposalRequestDto(asIs = true)
    is PlanAcceptance.WithEdits -> AcceptPlanProposalRequestDto(
        asIs = false,
        energyKcal = edits.targets.energyKcal,
        proteinG = edits.targets.proteinG,
        carbG = edits.targets.carbG,
        fatG = edits.targets.fatG,
        guidelines = PlanGuidelineCode.entries.filter { it in edits.guidelines }.map { it.code },
        patientMessage = edits.patientMessage?.text?.trim(),
    )
}
