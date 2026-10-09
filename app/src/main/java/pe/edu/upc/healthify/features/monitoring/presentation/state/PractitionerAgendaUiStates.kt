package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.ReferralField
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationCode
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PreparationInstruction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Una fila de PR17.0 «Próximas consultas». */
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

/** Avisos de PR17.0 al cancelar. */
enum class AgendaDialog {
    CONFIRM_CANCEL,

    /** `404`/`409`: ya no está agendada (pasó, se completó o ya se canceló). */
    NOT_CANCELLABLE,
    SERVER_ERROR,
}

/**
 * PR17.0 · Agenda (+ PR17.0-S, que muestra el shell). `visits = null` = todavía no se leyó. La agenda no se guarda en
 * el teléfono: sin conexión y sin lectura previa → estado offline.
 */
data class AgendaUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val visits: List<AgendaRow>? = null,
    /** Consulta tocada: hoja con «Reprogramar» y «Cancelar consulta». */
    val selected: AgendaRow? = null,
    val dialog: AgendaDialog? = null,
    val isCancelling: Boolean = false,
) {
    val isEmpty: Boolean get() = visits?.isEmpty() == true
    val needsConnection: Boolean get() = visits == null && loadFailed && isOffline
}

/** Paciente elegible en «Paciente» de PR17 (de la cartera, con vínculo activo). */
data class PatientOption(val patientId: Long, val fullName: String)

/** Por qué no sirve el paciente de PR17. */
enum class SchedulePatientError {
    MISSING,

    /** `409 PatientAlreadyHasActiveScheduledFollowUp`: «Este paciente ya tiene una consulta en agenda.» */
    ALREADY_SCHEDULED,

    /** `403 ActiveCareLinkRequired`: «No tienes un vínculo activo con este paciente». */
    NO_ACTIVE_LINK,
}

/** Por qué no sirve la fecha/hora de PR17. */
enum class ScheduleMomentError {
    MISSING,

    /** PR17.E · «Elige una fecha y hora futuras» (`400 ScheduledForMustBeInFuture`). */
    NOT_IN_FUTURE,
}

/**
 * PR17 · Agendar consulta (+ PR17.E y PR17.2). Con [rescheduleId] es la misma pantalla para reprogramar una consulta
 * de la agenda (paciente fijo). [failed] = PR17.2 «No se pudo agendar» (lo escrito no se pierde).
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

/** PR16 · Registrar derivación (+ PR16.E y PR16.2). [failed] = PR16.2 «Hubo un error» (lo escrito no se pierde). */
data class RecordReferralUiState(
    val specialty: String = "",
    val reason: String = "",
    val missing: Set<ReferralField> = emptySet(),
    val isSaving: Boolean = false,
    val failed: Boolean = false,
    val noActiveLink: Boolean = false,
    val isOffline: Boolean = false,
)
