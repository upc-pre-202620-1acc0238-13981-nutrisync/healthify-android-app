package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import java.time.Instant

/**
 * My visits (`PatientConsultationsOverviewResource`): the next visit, that visit's check-in and the past ones
 * (most recent first). No diagnosis, calculation basis, weight or BMI. A context that does not respond leaves its
 * section empty (`next = null`, `past` empty).
 */
data class ConsultationsOverview(
    val next: NextFollowUp?,
    /** `null` = not answered yet ("Reply"); with a value = the sent reply. */
    val checkIn: CheckInSummary?,
    val past: List<PastConsultation>,
) {
    /** "How did these weeks go?" card: there is a visit, not answered and still before its time. */
    fun canAnswerCheckIn(now: Instant): Boolean = next != null && checkIn == null && next.acceptsCheckInAt(now)

    /** "Edit my reply": locked if the backend says so or the visit's time has arrived. */
    fun canEditCheckIn(now: Instant): Boolean =
        next != null && checkIn != null && !checkIn.isLocked && next.acceptsCheckInAt(now)
}

/** A past visit (`PastConsultationResource`). */
data class PastConsultation(
    val id: Long,
    val date: Instant,
    val label: ConsultationLabel?,
    /** Plan version published in that visit, if any. */
    val planVersion: Int?,
)
