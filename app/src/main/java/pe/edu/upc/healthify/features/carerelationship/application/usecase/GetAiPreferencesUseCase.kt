package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/** Which AI features the patient can use (meal photo, meal ideas). Never fails: without data, none. */
class GetAiPreferencesUseCase @Inject constructor(
    private val repository: AiPreferencesRepository,
) {
    suspend operator fun invoke(patientUserId: Long): AiPreferences =
        repository.getAiPreferences(PatientId(patientUserId))
}
