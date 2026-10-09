package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import java.time.Instant

/**
 * Agenda y derivaciones del nutricionista (F25, F26, MA-2, MA-5). No se guardan en el teléfono: sin conexión se
 * muestra el estado offline del Figma.
 */
interface PractitionerAgendaRepository {

    /** `GET /practitioners/{uid}/scheduled-follow-ups?state=&from=` (solo la agenda propia). */
    suspend fun getAgenda(practitionerId: Long, state: FollowUpState?, from: Instant?): Result<List<AgendaVisit>>

    /**
     * `POST /scheduled-follow-ups`. `400 ScheduledForMustBeInFuture`, `403 ActiveCareLinkRequired` y
     * `409 PatientAlreadyHasActiveScheduledFollowUp` (una consulta en agenda por paciente).
     */
    suspend fun schedule(followUp: NewFollowUp): Result<AgendaVisit>

    /** `POST /scheduled-follow-ups/{id}/cancellation` (`204`). Solo mientras está agendada. */
    suspend fun cancel(followUpId: Long, reason: CancellationReason?): Result<Unit>

    /** `POST /scheduled-follow-ups/{id}/rescheduling`: otro momento futuro; la preparación se reemplaza. */
    suspend fun reschedule(followUpId: Long, scheduledFor: Instant, preparation: FollowUpPreparation): Result<AgendaVisit>

    /** `POST /referrals` (`201`). `400 SpecialtyAndReasonRequired`, `403 ActiveCareLinkRequired`. */
    suspend fun recordReferral(referral: NewReferral): Result<Unit>
}
