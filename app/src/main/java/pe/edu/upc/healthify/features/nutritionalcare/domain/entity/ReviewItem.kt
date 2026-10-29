package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import java.time.Instant
import java.time.LocalDate

/**
 * A review inbox item: a signal Monitoring raised that **awaits a human decision**.
 * Nothing here has changed a plan (*Signal Notifies Never Modifies The Plan*).
 *
 * @param legacyEvidence English sentence from the backend (`evidence`): shown only without structured [evidence].
 * @param evidence the evidence as numbers (`evidenceData`), which the app builds in the reader's language.
 * @param hasPlanProposal the AI attached (or is generating) a proposed plan: the item opens the AI proposal view.
 */
data class ReviewItem(
    val id: ReviewItemId,
    val patientId: PatientId,
    val patientFullName: String?,
    val signalType: SignalType,
    val legacyEvidence: String,
    val evidence: ReviewEvidence?,
    val isOpen: Boolean,
    val resolvedWithAdjustment: Boolean?,
    val resolutionNote: ResolutionNote?,
    val createdAt: Instant?,
    val resolvedAt: Instant?,
    val hasPlanProposal: Boolean,
) {
    /** Only an open sustained deviation can carry an AI-proposed plan. */
    val offersPlanProposal: Boolean get() = isOpen && hasPlanProposal && signalType == SignalType.SustainedDeviation
}

/**
 * Evidence of an item (`ReviewItemResource.evidenceData`). Days without logs **never count**: the
 * denominator is the logged days.
 */
sealed interface ReviewEvidence {

    /** "Logged on average 40 % below their energy target (≈ 718 kcal/day) on 5 of their last 9 logged days". */
    data class SustainedDeviation(
        /** Unsigned: how far it strayed on average (the sign goes in [direction]). */
        val averagePercentFromTarget: Double,
        val deviatedDays: Int,
        val loggedDaysConsidered: Int,
        val direction: DeviationDirection,
        /** Unsigned kcal/day; `null` on items that predate structured evidence. */
        val averageEnergyKcalFromTarget: Double?,
    ) : ReviewEvidence {
        init {
            require(averagePercentFromTarget >= 0) { "The percent is unsigned" }
            require(loggedDaysConsidered > 0) { "A deviation is measured over logged days" }
            require(deviatedDays in 1..loggedDaysConsidered) { "Deviated days are a part of the logged days" }
            require(averageEnergyKcalFromTarget == null || averageEnergyKcalFromTarget >= 0) { "The kcal are unsigned" }
        }
    }

    /** "Scheduled review after the Sep 8 adjustment (v3): 2 of 6 logged days off target". */
    data class ScheduledRecheck(
        val adjustedOn: LocalDate,
        val adjustedPlanVersion: Int,
        /** `null` (both) if there was no summary since the adjustment. */
        val deviatedDays: Int?,
        val loggedDaysConsidered: Int?,
    ) : ReviewEvidence {
        init {
            require(adjustedPlanVersion > 0) { "A plan version is positive" }
            require((deviatedDays == null) == (loggedDaysConsidered == null)) { "Both counts or none" }
            if (deviatedDays != null && loggedDaysConsidered != null) {
                require(loggedDaysConsidered >= 0 && deviatedDays in 0..loggedDaysConsidered) {
                    "Deviated days are a part of the logged days"
                }
            }
        }

        val hasCounts: Boolean get() = deviatedDays != null && loggedDaysConsidered != null
    }

    /**
     * "Consistency index: 0.46 kg/week unexplained; on alert since Aug 20 (4 weeks); shown to the
     * patient on Sep 9". Missing parts are left out.
     */
    data class ConsistencyEscalation(
        val kgPerWeek: Double,
        val alertSinceOn: LocalDate?,
        val weeksInAlert: Int?,
        val shownToPatientOn: LocalDate?,
    ) : ReviewEvidence {
        init {
            require(kgPerWeek >= 0) { "The index is unsigned" }
            require(weeksInAlert == null || weeksInAlert >= 0) { "Weeks in alert cannot be negative" }
        }
    }
}

/**
 * Note an item was resolved with (`resolutionNoteCode` + `resolutionNoteData`). A note written by the
 * practitioner ([Custom]) is shown as is and never translated.
 */
sealed interface ResolutionNote {
    data class PlanAssignedAsIs(val planVersion: Int) : ResolutionNote
    data class PlanAssignedWithEdits(val planVersion: Int) : ResolutionNote
    data object ProposalDiscarded : ResolutionNote

    data class Custom(val text: String) : ResolutionNote {
        init {
            require(text.isNotBlank()) { "A custom note needs text" }
        }
    }
}

/**
 * Resolution of a review item (`POST /review-items/{id}/resolution`). **Saying whether the plan was adjusted is
 * required**: without it there is no record of what the signal led to (*Resolution States Whether The Plan Was Adjusted*).
 */
@ConsistentCopyVisibility
data class ReviewResolution private constructor(val resolvedWithAdjustment: Boolean, val note: String?) {

    companion object {
        /** `null` if Yes or No was not answered ("Answer Yes or No"). An empty note is not sent. */
        fun of(resolvedWithAdjustment: Boolean?, note: String?): ReviewResolution? =
            resolvedWithAdjustment?.let { ReviewResolution(it, note?.trim()?.takeIf(String::isNotEmpty)) }
    }
}
