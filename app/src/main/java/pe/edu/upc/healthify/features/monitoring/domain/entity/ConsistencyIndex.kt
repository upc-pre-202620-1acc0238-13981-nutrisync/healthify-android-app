package pe.edu.upc.healthify.features.monitoring.domain.entity

/**
 * Consistency index (`ConsistencyIndexResource`): compares the weight trend with what was logged. Only the patient
 * reads it, and always sees it **before** any escalation (*Patient Prompt Required Before Escalation*).
 *
 * @param patientPromptPending there is a prompt the patient has not seen yet; the app acknowledges it when it draws
 *   the home card (`prompt-acknowledgement`).
 */
data class ConsistencyIndex(
    val state: ConsistencyState,
    val patientPromptPending: Boolean,
) {
    /**
     * The home screen shows "Something doesn't add up" while there is an active alert or an unseen prompt. `Watch`
     * is not shown: there is nothing to ask the patient yet.
     */
    val invitesPatientToReview: Boolean get() = patientPromptPending || state == ConsistencyState.ALERT
}

/** `Normal` · `Watch` · `Alert`. An unknown state reads as [NORMAL]: no needless warnings. */
enum class ConsistencyState(val code: String) {
    NORMAL("Normal"),
    WATCH("Watch"),
    ALERT("Alert"),
    ;

    companion object {
        fun fromCode(code: String?): ConsistencyState = entries.firstOrNull { it.code == code } ?: NORMAL
    }
}
