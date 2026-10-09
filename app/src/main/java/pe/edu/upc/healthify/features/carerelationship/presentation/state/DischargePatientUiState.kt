package pe.edu.upc.healthify.features.carerelationship.presentation.state

/** Avisos de PR18. */
enum class DischargeDialog {
    /** PR18.M · «¿DAR DE ALTA?». */
    CONFIRM,

    /** `409 DischargedLinkCannotBeReactivated`: «Este paciente ya había sido dado de alta.» */
    ALREADY_DISCHARGED,

    /** `403`/`404`: el vínculo no es de este profesional o ya no existe. */
    NO_ACTIVE_LINK,
    SERVER_ERROR,
}

/** PR18 · Alta clínica (+ PR18.M). */
data class DischargePatientUiState(
    val reason: String = "",
    val reasonMissing: Boolean = false,
    val dialog: DischargeDialog? = null,
    val isDischarging: Boolean = false,
    val isOffline: Boolean = false,
)
