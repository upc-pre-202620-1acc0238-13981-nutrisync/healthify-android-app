package pe.edu.upc.healthify.features.carerelationship.presentation.state

/** Discharge screen notices. */
enum class DischargeDialog {
    /** Confirmation "DISCHARGE?". */
    CONFIRM,

    /** `409 DischargedLinkCannotBeReactivated`: "This patient had already been discharged." */
    ALREADY_DISCHARGED,

    /** `403`/`404`: the care link does not belong to this practitioner or no longer exists. */
    NO_ACTIVE_LINK,
    SERVER_ERROR,
}

/** Clinical discharge (plus its confirmation). */
data class DischargePatientUiState(
    val reason: String = "",
    val reasonMissing: Boolean = false,
    val dialog: DischargeDialog? = null,
    val isDischarging: Boolean = false,
    val isOffline: Boolean = false,
)
