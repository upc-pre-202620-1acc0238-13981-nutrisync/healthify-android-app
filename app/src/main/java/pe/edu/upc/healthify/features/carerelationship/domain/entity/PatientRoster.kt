package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.RosterAttention
import java.time.Instant

/**
 * A patient in the nutritionist's roster (`PatientRosterItemResource`).
 *
 * @param isNew no baseline data or no published plan; [hasBaseline] decides whether the patient file opens on the
 *   baseline form or the overview.
 * @param nextFollowUpAt the next scheduled consultation, if any.
 * @param hasOpenReviewItem there is an open signal in the review inbox.
 */
data class RosterPatient(
    val patientId: Long,
    val careLinkId: CareLinkId,
    val fullName: String,
    val linkedSince: Instant,
    val isLinkActive: Boolean,
    val hasBaseline: Boolean,
    val activePlanVersion: Int?,
    val isNew: Boolean,
    val hasConsultationInProgress: Boolean,
    val nextFollowUpAt: Instant? = null,
    val hasOpenReviewItem: Boolean = false,
) {
    init {
        require(patientId > 0) { "PatientId must be positive" }
        require(fullName.isNotBlank()) { "A roster patient needs a name" }
    }

    /** The first reason, by priority ([RosterAttention]), why the patient needs action; `null` = up to date. */
    val attention: RosterAttention?
        get() = when {
            hasOpenReviewItem -> RosterAttention.OPEN_REVIEW
            hasConsultationInProgress -> RosterAttention.CONSULTATION_IN_PROGRESS
            !hasBaseline -> RosterAttention.MISSING_BASELINE
            isNew -> RosterAttention.PLAN_NOT_PUBLISHED
            else -> null
        }
}

/** The roster, in backend order. Empty = the first-time empty state. */
data class PatientRoster(val patients: List<RosterPatient>) {
    val isEmpty: Boolean get() = patients.isEmpty()
}
