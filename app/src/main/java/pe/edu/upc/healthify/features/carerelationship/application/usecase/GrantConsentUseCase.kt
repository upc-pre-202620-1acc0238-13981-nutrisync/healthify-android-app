package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * "I give my consent". The scope is the fixed, versioned text of the consent screen ([ConsentScope.CURRENT]); AI
 * is decided separately with the "Use AI features" switch.
 */
class GrantConsentUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        careLinkId: Long,
        aiProcessingGranted: Boolean,
    ): Result<CareLink> = careLinkRepository.grantConsent(
        patientId = PatientId(patientUserId),
        careLinkId = CareLinkId(careLinkId),
        scope = ConsentScope.CURRENT,
        aiProcessingGranted = aiProcessingGranted,
    )
}
