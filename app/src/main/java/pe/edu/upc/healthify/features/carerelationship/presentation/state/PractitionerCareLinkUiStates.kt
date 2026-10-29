package pe.edu.upc.healthify.features.carerelationship.presentation.state

import pe.edu.upc.healthify.features.carerelationship.domain.entity.RosterPatient
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationValidity
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.RosterAttention
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * Una fila de PR1.
 *
 * @param attention por qué requiere acción (el primer motivo por prioridad); `null` = al día.
 */
data class RosterItem(
    val patientId: Long,
    val careLinkId: Long,
    val fullName: String,
    val linkedSince: Instant,
    val isNew: Boolean,
    val attention: RosterAttention? = null,
    val hasConsultationInProgress: Boolean = false,
    val hasOpenReviewItem: Boolean = false,
    val nextFollowUpAt: Instant? = null,
) {
    companion object {
        fun of(patient: RosterPatient) = RosterItem(
            patientId = patient.patientId,
            careLinkId = patient.careLinkId.value,
            fullName = patient.fullName,
            linkedSince = patient.linkedSince,
            isNew = patient.isNew,
            attention = patient.attention,
            hasConsultationInProgress = patient.hasConsultationInProgress,
            hasOpenReviewItem = patient.hasOpenReviewItem,
            nextFollowUpAt = patient.nextFollowUpAt,
        )
    }
}

/**
 * PR1 · Mi cartera, agrupada por acción. `patients = null` = todavía no se leyó; lista vacía = PR1.V. Sin conexión y
 * sin lectura = PR1.O. El buscador filtra en el teléfono, sin llamada nueva.
 *
 * @param query lo escrito en «Buscar paciente…».
 * @param isUpToDateExpanded la fila «Al día» desplegada en su sitio.
 */
data class PatientRosterUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val patients: List<RosterItem>? = null,
    val query: String = "",
    val isUpToDateExpanded: Boolean = false,
) {
    val isEmpty: Boolean get() = patients?.isEmpty() == true
    val needsConnection: Boolean get() = patients == null && loadFailed && isOffline

    private val matching: List<RosterItem>
        get() {
            val term = query.searchable()
            return patients.orEmpty().filter { term.isEmpty() || it.fullName.searchable().contains(term) }
        }

    /** «Requieren acción»: ordenados por la prioridad de su motivo ([RosterAttention]); el resto, en el orden del backend. */
    val needsAction: List<RosterItem> get() = matching.filter { it.attention != null }.sortedBy { it.attention?.ordinal }

    /** «Al día»: sin pendientes. */
    val upToDate: List<RosterItem> get() = matching.filter { it.attention == null }

    /** Hay cartera, pero nadie coincide con la búsqueda. */
    val noSearchResults: Boolean get() = query.isNotBlank() && !patients.isNullOrEmpty() && matching.isEmpty()
}

/** «Ramírez» se encuentra escribiendo «ramirez»: sin mayúsculas ni tildes. */
private fun String.searchable(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("\\p{Mn}+")

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
