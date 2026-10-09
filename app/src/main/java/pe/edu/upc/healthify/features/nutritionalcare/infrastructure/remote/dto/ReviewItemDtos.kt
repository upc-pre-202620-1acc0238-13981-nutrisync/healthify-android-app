package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `ReviewItemResource` (F27, NC-11, X-2). */
@Serializable
data class ReviewItemDto(
    val reviewItemId: Long,
    val patientId: Long,
    val practitionerId: Long,
    val signalType: String,
    val evidence: String = "",
    val state: String,
    val resolvedWithAdjustment: Boolean? = null,
    val resolutionNote: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String? = null,
    val patientFullName: String? = null,
    val hasPlanProposal: Boolean = false,
    val evidenceData: ReviewItemEvidenceDto? = null,
    val resolutionNoteCode: String? = null,
    val resolutionNoteData: ResolutionNoteDataDto? = null,
)

/** `ReviewItemEvidenceResource`: los campos de cada `signalType` (los demás llegan `null`). */
@Serializable
data class ReviewItemEvidenceDto(
    val averagePercentFromTarget: Double? = null,
    val deviatedDays: Int? = null,
    val loggedDaysConsidered: Int? = null,
    val direction: String? = null,
    val adjustedOn: String? = null,
    val adjustedPlanVersion: Int? = null,
    val averageEnergyKcalFromTarget: Double? = null,
    val consistencyKgPerWeek: Double? = null,
    val consistencyState: String? = null,
    val alertSinceOn: String? = null,
    val weeksInAlert: Int? = null,
    val shownToPatientOn: String? = null,
)

@Serializable
data class ResolutionNoteDataDto(val planVersion: Int? = null)

/** `ResolveReviewItemResource`. */
@Serializable
data class ResolveReviewItemRequestDto(val resolvedWithAdjustment: Boolean, val resolutionNote: String?)

/** `PlanAdjustmentProposalResource` (NC-10, X-2). */
@Serializable
data class PlanAdjustmentProposalDto(
    val proposalId: Long,
    val reviewItemId: Long,
    val title: String,
    val currentEnergyKcal: Double? = null,
    val proposedEnergyKcal: Double,
    val proposedProteinG: Double,
    val proposedCarbG: Double,
    val proposedFatG: Double,
    val addedGuidelines: List<String> = emptyList(),
    val removedGuidelines: List<String> = emptyList(),
    val patientMessage: String = "",
    val recheckAfterDays: Int,
    val rationale: String = "",
    val generatedAt: String,
    val status: String,
    val assignedPlanVersion: Int? = null,
    val practitionerLanguage: String? = null,
    val patientLanguage: String? = null,
)

/** `AcceptPlanProposalResource`: `{ asIs: true }` o las ediciones completas. */
@Serializable
data class AcceptPlanProposalRequestDto(
    val asIs: Boolean,
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val carbG: Double? = null,
    val fatG: Double? = null,
    val guidelines: List<String>? = null,
    val patientMessage: String? = null,
)

/** `PlanProposalAcceptanceResource`. */
@Serializable
data class PlanProposalAcceptanceDto(val reviewItem: ReviewItemDto, val planVersion: NutritionPlanDto)
