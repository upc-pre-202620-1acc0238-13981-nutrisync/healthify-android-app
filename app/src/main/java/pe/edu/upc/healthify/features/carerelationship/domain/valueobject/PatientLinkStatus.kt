package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * Where the patient stands with their nutritionist. Decides where the patient goes on entry:
 * signing up grants access to nothing; the care link starts inactive when the invitation is redeemed and only
 * consent activates it.
 */
enum class PatientLinkStatus {
    /** No open care link: scan the invitation. */
    NO_LINK,

    /** Invitation redeemed, consent still missing. */
    PENDING_CONSENT,

    /** Active care link: Home. */
    ACTIVE,
}
