package pe.edu.upc.healthify.features.carerelationship.domain.entity

import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import java.time.Instant

/**
 * Un paciente de la cartera del nutricionista (`PatientRosterItemResource`, RM-1, PR1).
 *
 * @param isNew sin datos base o sin plan publicado: la ficha abre en PAC-0 o PAC-1 según [hasBaseline].
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
) {
    init {
        require(patientId > 0) { "PatientId must be positive" }
        require(fullName.isNotBlank()) { "A roster patient needs a name" }
    }
}

/** PR1 · la cartera, en el orden del backend. Vacía = PR1.V (primera vez). */
data class PatientRoster(val patients: List<RosterPatient>) {
    val isEmpty: Boolean get() = patients.isEmpty()
}
