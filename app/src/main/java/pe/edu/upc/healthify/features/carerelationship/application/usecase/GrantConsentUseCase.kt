package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT2 · «Doy mi consentimiento» (F6). El alcance es el texto fijo y versionado de PT2 ([ConsentScope.CURRENT]); la IA
 * se decide por separado con el interruptor «Usar funciones con IA» (CR-2).
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
