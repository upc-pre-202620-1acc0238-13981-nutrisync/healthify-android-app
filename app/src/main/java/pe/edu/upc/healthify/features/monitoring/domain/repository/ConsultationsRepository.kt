package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** The patient's visits, their check-in and suggested questions. Nothing is stored on the phone. */
interface ConsultationsRepository {

    /** `GET /patients/{pid}/consultations-overview`. */
    suspend fun getOverview(patientId: PatientId): Result<ConsultationsOverview>

    /** `GET /scheduled-follow-ups/{id}/check-in`; `null` while not answered (`404 PreVisitCheckInNotFound`). */
    suspend fun getCheckIn(followUpId: FollowUpId): Result<PreVisitCheckIn?>

    /**
     * `PUT /scheduled-follow-ups/{id}/check-in`: creates or replaces the reply. `409 CheckInLocked` /
     * `FollowUpNotScheduled` if no longer possible; `404` if the visit is not this patient's.
     */
    suspend fun submitCheckIn(followUpId: FollowUpId, answer: CheckInAnswer): Result<PreVisitCheckIn>

    /** `GET /patients/{pid}/suggested-questions?followUpId=`. Offline or with the provider down -> error. */
    suspend fun getSuggestedQuestions(
        patientId: PatientId,
        followUpId: FollowUpId?,
    ): Result<SuggestedQuestionsAvailability>
}
