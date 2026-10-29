package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Why a roster patient needs something from the nutritionist. Declaration order is the priority: if
 * several apply, the first one counts, and the "Need action" card sorts its rows by the same rule.
 */
enum class RosterAttention {
    /** There is an open signal in the review inbox (`hasOpenReviewItem`). */
    OPEN_REVIEW,

    /** A guided consultation was left half done (`hasConsultationInProgress`). */
    CONSULTATION_IN_PROGRESS,

    /** No baseline data yet (`!hasBaseline`). */
    MISSING_BASELINE,

    /** Has baseline data but no published plan (`isNew`). */
    PLAN_NOT_PUBLISHED,
}
