package pe.edu.upc.healthify.features.carerelationship.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.RosterPatient
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import java.time.Duration
import java.time.Instant

/** Una fila de PR1. */
data class RosterItem(
    val patientId: Long,
    val careLinkId: Long,
    val fullName: String,
    val linkedSince: Instant,
    val isNew: Boolean,
) {
    companion object {
        fun of(patient: RosterPatient) = RosterItem(
            patientId = patient.patientId,
            careLinkId = patient.careLinkId.value,
            fullName = patient.fullName,
            linkedSince = patient.linkedSince,
            isNew = patient.isNew,
        )
    }
}

/**
 * PR1 · Mi cartera. `patients = null` = todavía no se leyó; lista vacía = PR1.V. Sin conexión y sin lectura = PR1.O.
 */
data class PatientRosterUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val patients: List<RosterItem>? = null,
) {
    val isEmpty: Boolean get() = patients?.isEmpty() == true
    val needsConnection: Boolean get() = patients == null && loadFailed && isOffline
}

/** Por qué no se pudo generar el código de PR2 (textos de las Notas). */
enum class InviteError { OFFLINE, EXPIRATION_REQUIRED, GENERIC }

/**
 * PR2 · Generar invitación (+ PR2.L). El token vive solo en memoria mientras la pantalla está abierta: nunca se guarda
 * ni se vuelve a pedir.
 */
data class InvitePatientUiState(
    val validity: InvitationValidity = InvitationValidity.DEFAULT,
    val isGenerating: Boolean = true,
    val token: InvitationToken? = null,
    val remaining: Duration? = null,
    val isExpired: Boolean = false,
    val isOffline: Boolean = false,
    val error: InviteError? = null,
)
