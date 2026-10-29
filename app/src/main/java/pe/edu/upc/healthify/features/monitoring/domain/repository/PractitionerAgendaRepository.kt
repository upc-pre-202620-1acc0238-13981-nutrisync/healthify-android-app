package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import java.time.Instant

/**
 * The nutritionist's agenda and referrals. Not stored on the phone: offline, the Figma offline state is shown.
 */
interface PractitionerAgendaRepository {

    /** `GET /practitioners/{uid}/scheduled-follow-ups?state=&from=` (own agenda only). */
    suspend fun getAgenda(practitionerId: Long, state: FollowUpState?, from: Instant?): Result<List<AgendaVisit>>

    /**
     * `POST /scheduled-follow-ups`. `400 ScheduledForMustBeInFuture`, `403 ActiveCareLinkRequired` and
     * `409 PatientAlreadyHasActiveScheduledFollowUp` (one agenda visit per patient).
     */
    suspend fun schedule(followUp: NewFollowUp): Result<AgendaVisit>

    /** `POST /scheduled-follow-ups/{id}/cancellation` (`204`). Only while it is scheduled. */
    suspend fun cancel(followUpId: Long, reason: CancellationReason?): Result<Unit>

    /** `POST /scheduled-follow-ups/{id}/rescheduling`: another future moment; the preparation is replaced. */
    suspend fun reschedule(followUpId: Long, scheduledFor: Instant, preparation: FollowUpPreparation): Result<AgendaVisit>

    /** `POST /referrals` (`201`). `400 SpecialtyAndReasonRequired`, `403 ActiveCareLinkRequired`. */
    suspend fun recordReferral(referral: NewReferral): Result<Unit>
}
