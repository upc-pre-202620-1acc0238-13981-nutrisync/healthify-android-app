package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/** Qué funciones con IA puede usar el paciente (PT5 foto, PT14 ideas). Nunca falla: sin datos, ninguna. */
class GetAiPreferencesUseCase @Inject constructor(
    private val repository: AiPreferencesRepository,
) {
    suspend operator fun invoke(patientUserId: Long): AiPreferences =
        repository.getAiPreferences(PatientId(patientUserId))
}
