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
 * `ReviewItemResource` → [ReviewItem]. The evidence is built from `evidenceData` per `signalType`; if a required
 * field is missing (or the item predates structured evidence) it stays `null` and the UI shows the legacy `evidence`.
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

/** The evidence for [type]; `null` if its fields are missing (falls back to the legacy sentence, never a partial one). */
internal fun ReviewItemEvidenceDto.toEvidence(type: SignalType): ReviewEvidence? = try {
    when (type) {
        SignalType.SustainedDeviation -> {
            val percent = averagePercentFromTarget
            val deviated = deviatedDays
            val logged = loggedDaysConsidered
            // The direction comes in `direction`; if missing, the sign of the percentage gives it.
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
    // Numbers that break an invariant (e.g. more deviated days than logged ones): the legacy sentence is better.
    null
}

private fun ReviewItemDto.resolutionNote(): ResolutionNote? {
    val version = resolutionNoteData?.planVersion
    val text = resolutionNote?.trim()?.takeIf(String::isNotEmpty)
    return when (resolutionNoteCode?.trim()) {
        "PlanAssignedAsIs" -> version?.let(ResolutionNote::PlanAssignedAsIs) ?: text?.let(ResolutionNote::Custom)
        "PlanAssignedWithEdits" -> version?.let(ResolutionNote::PlanAssignedWithEdits) ?: text?.let(ResolutionNote::Custom)
        "ProposalDiscarded" -> ResolutionNote.ProposalDiscarded
        // `Custom`, an unknown code or none: the text as is.
        else -> text?.let(ResolutionNote::Custom)
    }
}

fun ReviewResolution.toDto(): ResolveReviewItemRequestDto = ResolveReviewItemRequestDto(resolvedWithAdjustment, note)

/** A guideline this version does not know is left out (the AI can only return catalog codes). */
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
