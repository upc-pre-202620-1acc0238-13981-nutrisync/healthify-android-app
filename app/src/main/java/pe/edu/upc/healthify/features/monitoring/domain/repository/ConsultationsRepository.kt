package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** Consultas del paciente (RM-5), su check-in (MA-4) y las preguntas sugeridas (IA-4). Nada se guarda en el teléfono. */
interface ConsultationsRepository {

    /** `GET /patients/{pid}/consultations-overview`. */
    suspend fun getOverview(patientId: PatientId): Result<ConsultationsOverview>

    /** `GET /scheduled-follow-ups/{id}/check-in`; `null` mientras no se respondió (`404 PreVisitCheckInNotFound`). */
    suspend fun getCheckIn(followUpId: FollowUpId): Result<PreVisitCheckIn?>

    /**
     * `PUT /scheduled-follow-ups/{id}/check-in`: crea o reemplaza la respuesta. `409 CheckInLocked` /
     * `FollowUpNotScheduled` si ya no se puede; `404` si la consulta no es de este paciente.
     */
    suspend fun submitCheckIn(followUpId: FollowUpId, answer: CheckInAnswer): Result<PreVisitCheckIn>

    /** `GET /patients/{pid}/suggested-questions?followUpId=`. Sin red o con el proveedor caído → error. */
    suspend fun getSuggestedQuestions(
        patientId: PatientId,
        followUpId: FollowUpId?,
    ): Result<SuggestedQuestionsAvailability>
}
