package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationsRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** PT25 · Mis consultas: próxima, check-in y anteriores. */
class GetConsultationsOverviewUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ConsultationsOverview> =
        repository.getOverview(PatientId(patientUserId))
}

/** PT25.2 «Editar mi respuesta»: la respuesta guardada (`null` si todavía no hay). */
class GetCheckInUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(followUpId: Long): Result<PreVisitCheckIn?> =
        repository.getCheckIn(FollowUpId(followUpId))
}

/** PT25.2 «Enviar a mi nutricionista»: crea o reemplaza la respuesta (MA-4). */
class SubmitCheckInUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(followUpId: Long, answer: CheckInAnswer): Result<PreVisitCheckIn> =
        repository.submitCheckIn(FollowUpId(followUpId), answer)
}

/** PT25 «Prepara tu consulta» y PT25.2 «También podrías preguntar» (IA-4). */
class GetSuggestedQuestionsUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(patientUserId: Long, followUpId: Long?): Result<SuggestedQuestionsAvailability> =
        repository.getSuggestedQuestions(PatientId(patientUserId), followUpId?.let(::FollowUpId))
}
