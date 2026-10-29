package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import java.time.Instant

/** Where a proposal ended up (`PlanProposalStatus`). Only [PROPOSED] can be accepted. */
enum class PlanProposalStatus(val code: String) {
    PROPOSED("Proposed"),
    ACCEPTED_AS_IS("AcceptedAsIs"),
    ACCEPTED_WITH_EDITS("AcceptedWithEdits"),
    DISMISSED("Dismissed"),
    ;

    companion object {
        /** An unknown status reads as decided: the app never offers to accept something it does not understand. */
        fun fromCode(code: String?): PlanProposalStatus =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: DISMISSED
    }
}

/**
 * Plan the AI proposes after a sustained deviation (`PlanAdjustmentProposalResource`). Reading it never changes
 * the plan: the only way to a new version is for the practitioner to accept it.
 *
 * @param title and [rationale] come in the practitioner's language; [patientMessage] in the patient's.
 * @param addedGuidelines and [removedGuidelines] are codes from the closed catalog (the AI cannot return others).
 */
data class PlanAdjustmentProposal(
    val reviewItemId: ReviewItemId,
    val title: String,
    val currentEnergyKcal: Double?,
    val proposed: Targets,
    val addedGuidelines: List<PlanGuidelineCode>,
    val removedGuidelines: List<PlanGuidelineCode>,
    val patientMessage: String,
    val recheckAfterDays: Int,
    val rationale: String,
    val generatedAt: Instant,
    val status: PlanProposalStatus,
) {
    init {
        require(title.isNotBlank()) { "A proposal has a title" }
        require(recheckAfterDays > 0) { "The recheck is in the future" }
    }

    val canBeAccepted: Boolean get() = status == PlanProposalStatus.PROPOSED

    /**
     * Catalog guidelines the version ends up with if accepted as is: the current ones minus the removed plus the
     * added (the practitioner's custom ones are always kept by the backend and are not edited here).
     */
    fun resultingGuidelines(current: Collection<PlanGuidelineCode>): Set<PlanGuidelineCode> =
        (current.filterNot { it in removedGuidelines } + addedGuidelines).toCollection(LinkedHashSet())
}

/** What `GET /review-items/{id}/plan-proposal` returns. */
sealed interface PlanProposalLookup {
    data class Ready(val proposal: PlanAdjustmentProposal) : PlanProposalLookup

    /** `202` + `Retry-After`: being generated. */
    data class Generating(val retryAfterSeconds: Int) : PlanProposalLookup {
        init {
            require(retryAfterSeconds > 0) { "Retry-After is positive" }
        }
    }

    /** `404`: no proposal (AI off, no consent, provider down or output rejected) → the item opens without AI. */
    data object None : PlanProposalLookup
}

/** "Resolve" on the AI proposal (as is) or "Assign adjusted plan" when adjusting it (with edits). */
sealed interface PlanAcceptance {
    data object AsIs : PlanAcceptance

    data class WithEdits(val edits: ProposalEdits) : PlanAcceptance
}

/**
 * Edits made when adjusting the AI proposal. Only the backend checks them against the calorie floor
 * (`422 PlanProposalOutOfSafetyBounds`); here the targets must be positive, the minimum the backend accepts.
 *
 * @param guidelines the **complete** list of catalog guidelines the practitioner keeps.
 */
data class ProposalEdits(
    val targets: Targets,
    val guidelines: Set<PlanGuidelineCode>,
    val patientMessage: PatientMessage?,
) {
    init {
        require(targets.proteinG > 0 && targets.carbG > 0 && targets.fatG > 0) { "Edited macros must be positive" }
    }
}

/** The item is resolved and version [planVersion] is the current one (`PlanProposalAcceptanceResource`). */
data class AcceptedProposal(val reviewItem: ReviewItem, val planVersion: Int)

/** A field of the adjust form with an invalid value. */
enum class ProposalEditField { ENERGY, PROTEIN, CARB, FAT, MESSAGE }

/** Validation of the adjust form (fail fast before sending). */
sealed interface ProposalEditValidation {
    data class Valid(val edits: ProposalEdits) : ProposalEditValidation
    data class Invalid(val fields: Set<ProposalEditField>) : ProposalEditValidation

    companion object {
        fun of(
            energyText: String,
            proteinText: String,
            carbText: String,
            fatText: String,
            guidelines: Set<PlanGuidelineCode>,
            message: String,
        ): ProposalEditValidation {
            val energy = parsePositive(energyText)
            val protein = parsePositive(proteinText)
            val carb = parsePositive(carbText)
            val fat = parsePositive(fatText)
            val trimmedMessage = message.trim()
            val invalid = buildSet {
                if (energy == null) add(ProposalEditField.ENERGY)
                if (protein == null) add(ProposalEditField.PROTEIN)
                if (carb == null) add(ProposalEditField.CARB)
                if (fat == null) add(ProposalEditField.FAT)
                if (trimmedMessage.length > PatientMessage.MAX_LENGTH) add(ProposalEditField.MESSAGE)
            }
            if (invalid.isNotEmpty() || energy == null || protein == null || carb == null || fat == null) {
                return Invalid(invalid)
            }
            return Valid(
                ProposalEdits(
                    targets = Targets(energy, protein, carb, fat),
                    guidelines = guidelines,
                    patientMessage = PatientMessage.ofOptional(trimmedMessage),
                ),
            )
        }

        /** "1 650", "1650", "62,5" or "62.5"; `null` if it is not a positive number. */
        internal fun parsePositive(text: String): Double? {
            val normalized = text.filterNot { it.isWhitespace() || it == ' ' || it == ' ' }.replace(',', '.')
            return normalized.toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
        }
    }
}
