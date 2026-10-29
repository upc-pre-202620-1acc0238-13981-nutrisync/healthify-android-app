package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * Whether there is a plan version the patient has not reviewed yet. Reads the active care link
 * (`/care-links/active`) and its `targets-read-status`.
 */
class GetTargetsReadStatusUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<TargetsReadStatus> {
        val link = careLinkRepository.getActiveCareLink(PatientId(patientUserId))
            .getOrElse { return Result.failure(it) }
        return careLinkRepository.getTargetsReadStatus(link.id)
    }
}
