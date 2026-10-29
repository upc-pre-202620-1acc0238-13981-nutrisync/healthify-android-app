package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationsRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** My visits: next, check-in and past ones. */
class GetConsultationsOverviewUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ConsultationsOverview> =
        repository.getOverview(PatientId(patientUserId))
}

/** "Edit my reply": the saved reply (`null` if there is none yet). */
class GetCheckInUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(followUpId: Long): Result<PreVisitCheckIn?> =
        repository.getCheckIn(FollowUpId(followUpId))
}

/** "Send to my nutritionist": creates or replaces the reply. */
class SubmitCheckInUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(followUpId: Long, answer: CheckInAnswer): Result<PreVisitCheckIn> =
        repository.submitCheckIn(FollowUpId(followUpId), answer)
}

/** "Prepare for your visit" and "You could also ask". */
class GetSuggestedQuestionsUseCase @Inject constructor(
    private val repository: ConsultationsRepository,
) {
    suspend operator fun invoke(patientUserId: Long, followUpId: Long?): Result<SuggestedQuestionsAvailability> =
        repository.getSuggestedQuestions(PatientId(patientUserId), followUpId?.let(::FollowUpId))
}
