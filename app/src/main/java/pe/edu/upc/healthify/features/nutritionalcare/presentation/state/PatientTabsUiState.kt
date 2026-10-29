package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep

/**
 * Summary tab. `summary.baseline == null` means no baseline yet; it may also carry a consultation in progress.
 * None of this is stored on the phone: offline with no previous read, it asks for a connection.
 */
data class PatientSummaryUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val summary: PatientSummary? = null,
    val isStartingConsultation: Boolean = false,
    val showStartError: Boolean = false,
)

/** Navigation requested by the patient profile tabs. */
sealed interface PatientTabEvent {
    /** Record or edit the baseline. */
    data class OpenBaseline(val editing: Boolean) : PatientTabEvent

    /** The step the consultation is on. */
    data class OpenConsultationStep(val step: ConsultationStep) : PatientTabEvent
}

/** Record tab (with the active diagnosis, practitioner only). */
data class PractitionerRecordUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val record: PractitionerPatientRecord? = null,
    /** The assessment open in its detail sheet; `null` with no sheet. */
    val evaluationSheet: EvaluationSheetState? = null,
)

/**
 * Plan tab. [readStatus] tells whether the patient saw the published targets ("Ana saw them the same day"); if it
 * could not be read, that line is hidden.
 */
data class PatientPlanUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val history: PlanHistory? = null,
    val readStatus: TargetsReadStatus? = null,
    val isStartingConsultation: Boolean = false,
    val showStartError: Boolean = false,
)
