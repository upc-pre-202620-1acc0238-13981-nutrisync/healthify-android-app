package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep

/**
 * PAC-0 / PAC-1 / PAC-1.C · pestaña Resumen. `summary.baseline == null` es PAC-0; con consulta en curso, PAC-1.C. Nada
 * de esto se guarda en el teléfono: sin conexión y sin lectura previa se pide conexión.
 */
data class PatientSummaryUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val summary: PatientSummary? = null,
    val isStartingConsultation: Boolean = false,
    val showStartError: Boolean = false,
)

/** Navegación pedida por las pestañas de la ficha del paciente. */
sealed interface PatientTabEvent {
    /** EV-1 · registrar (PAC-0) o editar los datos base. */
    data class OpenBaseline(val editing: Boolean) : PatientTabEvent

    /** EV-2 a EV-5 · el paso donde quedó la consulta. */
    data class OpenConsultationStep(val step: ConsultationStep) : PatientTabEvent
}

/** PAC-3 · pestaña Expediente (con el diagnóstico activo, solo del profesional). */
data class PractitionerRecordUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val record: PractitionerPatientRecord? = null,
)

/**
 * PAC-4 · pestaña Plan. [readStatus] dice si el paciente vio las metas publicadas («Ana las vio el mismo día»); si no
 * se pudo leer, esa línea no se muestra.
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
