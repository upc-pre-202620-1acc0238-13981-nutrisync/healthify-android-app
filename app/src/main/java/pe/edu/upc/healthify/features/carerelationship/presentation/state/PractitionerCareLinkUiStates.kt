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
 * A roster row.
 *
 * @param attention why the patient needs action (the first reason by priority); `null` = up to date.
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
 * My roster, grouped by action. `patients = null` = not read yet; empty list = empty state. Offline with
 * no read = offline state. The search filters on the phone, without a new call.
 *
 * @param query what was typed in "Search patient…".
 * @param isUpToDateExpanded the "Up to date" row expanded in place.
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

    /** "Need action": sorted by the priority of their reason ([RosterAttention]); the rest, in backend order. */
    val needsAction: List<RosterItem> get() = matching.filter { it.attention != null }.sortedBy { it.attention?.ordinal }

    /** "Up to date": nothing pending. */
    val upToDate: List<RosterItem> get() = matching.filter { it.attention == null }

    /** There is a roster, but nobody matches the search. */
    val noSearchResults: Boolean get() = query.isNotBlank() && !patients.isNullOrEmpty() && matching.isEmpty()
}

/** "Ramírez" is found by typing "ramirez": case- and accent-insensitive. */
private fun String.searchable(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase(Locale.ROOT)

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/** Why the invitation code could not be generated (texts from the design notes). */
enum class InviteError { OFFLINE, EXPIRATION_REQUIRED, GENERIC }

/**
 * Generate invitation (plus its loading state). The token lives only in memory while the screen is open: it is
 * never saved or requested again.
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
