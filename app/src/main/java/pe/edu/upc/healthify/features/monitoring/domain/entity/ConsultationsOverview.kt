package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationLabel
import java.time.Instant

/**
 * PT25 · Mis consultas (`PatientConsultationsOverviewResource`, RM-5): la próxima consulta, el check-in de esa
 * consulta y las ya realizadas (la más reciente primero). Sin diagnóstico, base de cálculo, peso ni IMC. Un contexto
 * que no responde deja su sección vacía (`next = null`, `past` vacía).
 */
data class ConsultationsOverview(
    val next: NextFollowUp?,
    /** `null` = todavía sin responder (PT25 «Responder»); con valor = PT25.3. */
    val checkIn: CheckInSummary?,
    val past: List<PastConsultation>,
) {
    /** PT25 card «¿Cómo te fue estas semanas?»: hay consulta, sin responder y todavía antes de su hora. */
    fun canAnswerCheckIn(now: Instant): Boolean = next != null && checkIn == null && next.acceptsCheckInAt(now)

    /** PT25.3 «Editar mi respuesta»: bloqueado si el backend lo dice o si ya llegó la hora de la consulta. */
    fun canEditCheckIn(now: Instant): Boolean =
        next != null && checkIn != null && !checkIn.isLocked && next.acceptsCheckInAt(now)
}

/** Una consulta ya realizada (`PastConsultationResource`). */
data class PastConsultation(
    val id: Long,
    val date: Instant,
    val label: ConsultationLabel?,
    /** Versión del plan que se publicó en esa consulta, si hubo. */
    val planVersion: Int?,
)
