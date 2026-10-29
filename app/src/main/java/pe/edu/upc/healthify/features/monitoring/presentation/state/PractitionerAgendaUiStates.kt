package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralField
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** A row of "Upcoming visits". */
data class AgendaRow(
    val followUpId: Long,
    val patientId: Long,
    val patientName: String?,
    val scheduledFor: Instant,
    val preparation: Set<PreparationCode>,
) {
    companion object {
        fun of(visit: AgendaVisit) = AgendaRow(
            followUpId = visit.id,
            patientId = visit.patientId.value,
            patientName = visit.patientFullName,
            scheduledFor = visit.scheduledFor,
            preparation = visit.preparation.filterIsInstance<PreparationInstruction.Catalog>().map { it.code }.toSet(),
        )
    }
}

/** Agenda notices when cancelling. */
enum class AgendaDialog {
    CONFIRM_CANCEL,

    /** `404`/`409`: no longer scheduled (past, completed or already cancelled). */
    NOT_CANCELLABLE,
    SERVER_ERROR,
}

/**
 * Agenda (its Snackbar is shown by the shell). `visits = null` = not read yet. The agenda is not stored on the
 * phone: offline with no previous read -> offline state.
 */
data class AgendaUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val visits: List<AgendaRow>? = null,
    /** Tapped visit: sheet with "Reschedule" and "Cancel visit". */
    val selected: AgendaRow? = null,
    val dialog: AgendaDialog? = null,
    val isCancelling: Boolean = false,
) {
    val isEmpty: Boolean get() = visits?.isEmpty() == true
    val needsConnection: Boolean get() = visits == null && loadFailed && isOffline
}

/** Patient selectable in the schedule form's "Patient" field (from the roster, with an active care link). */
data class PatientOption(val patientId: Long, val fullName: String)

/** Why the schedule form's patient is not valid. */
enum class SchedulePatientError {
    MISSING,

    /** `409 PatientAlreadyHasActiveScheduledFollowUp`: "This patient already has a visit on the agenda." */
    ALREADY_SCHEDULED,

    /** `403 ActiveCareLinkRequired`: "You don't have an active care link with this patient". */
    NO_ACTIVE_LINK,
}

/** Why the schedule form's date/time is not valid. */
enum class ScheduleMomentError {
    MISSING,

    /** "Pick a future date and time" (`400 ScheduledForMustBeInFuture`). */
    NOT_IN_FUTURE,
}

/**
 * Schedule a visit (with validation and error states). With [rescheduleId] it is the same screen to reschedule an
 * agenda visit (fixed patient). [failed] = "Could not schedule" (what was written is kept).
 */
data class ScheduleFollowUpUiState(
    val patient: PatientOption? = null,
    val patientFixed: Boolean = false,
    val patients: List<PatientOption>? = null,
    val isLoadingPatients: Boolean = false,
    val showPatientPicker: Boolean = false,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val preparation: Set<PreparationCode> = emptySet(),
    val patientError: SchedulePatientError? = null,
    val momentError: ScheduleMomentError? = null,
    val isSaving: Boolean = false,
    val failed: Boolean = false,
    val isOffline: Boolean = false,
    val rescheduleId: Long? = null,
) {
    val isRescheduling: Boolean get() = rescheduleId != null
}

/** Record a referral (with validation and error states). [failed] = "There was an error" (what was written is kept). */
data class RecordReferralUiState(
    val specialty: String = "",
    val reason: String = "",
    val missing: Set<ReferralField> = emptySet(),
    val isSaving: Boolean = false,
    val failed: Boolean = false,
    val noActiveLink: Boolean = false,
    val isOffline: Boolean = false,
)
