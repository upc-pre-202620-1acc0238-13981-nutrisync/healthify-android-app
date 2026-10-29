package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.RosterAttention
import java.time.Instant

/**
 * Un paciente de la cartera del nutricionista (`PatientRosterItemResource`, RM-1, PR1).
 *
 * @param isNew sin datos base o sin plan publicado: la ficha abre en PAC-0 o PAC-1 según [hasBaseline].
 * @param nextFollowUpAt la próxima consulta agendada, si hay.
 * @param hasOpenReviewItem hay una señal abierta en la bandeja de revisión.
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

    /** PR1 · el primer motivo, por prioridad ([RosterAttention]), por el que requiere acción; `null` = al día. */
    val attention: RosterAttention?
        get() = when {
            hasOpenReviewItem -> RosterAttention.OPEN_REVIEW
            hasConsultationInProgress -> RosterAttention.CONSULTATION_IN_PROGRESS
            !hasBaseline -> RosterAttention.MISSING_BASELINE
            isNew -> RosterAttention.PLAN_NOT_PUBLISHED
            else -> null
        }
}

/** PR1 · la cartera, en el orden del backend. Vacía = PR1.V (primera vez). */
data class PatientRoster(val patients: List<RosterPatient>) {
    val isEmpty: Boolean get() = patients.isEmpty()
}
