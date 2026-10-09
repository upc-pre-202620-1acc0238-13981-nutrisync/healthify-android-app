package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT3.M / PT4: si hay una versión del plan que el paciente todavía no revisó. Lee el vínculo activo
 * (`/care-links/active`) y su `targets-read-status`.
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
