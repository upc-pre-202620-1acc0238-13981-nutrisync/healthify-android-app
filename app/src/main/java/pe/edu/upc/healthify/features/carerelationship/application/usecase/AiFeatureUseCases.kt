package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiFeature
import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT21.IA: enciende o apaga una función con IA (IA-1). Encender una sin el consentimiento de IA no se pide al backend
 * (respondería `409 AiConsentRequiredToEnableFeature`): falla igual en el teléfono y la UI ofrece activarlo.
 * Apagarla siempre se puede y no cambia el plan ni los registros.
 */
class ChangeAiFeatureUseCase @Inject constructor(
    private val repository: AiPreferencesRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        current: AiPreferences,
        feature: AiFeature,
        enabled: Boolean,
    ): Result<AiPreferences> {
        if (enabled && !current.consentGranted) return domainFailure(DomainError.Conflict(CODE_CONSENT_REQUIRED))
        if (current.isEnabled(feature) == enabled) return Result.success(current)
        return repository.updateAiPreferences(PatientId(patientUserId), current.with(feature, enabled))
    }

    companion object {
        const val CODE_CONSENT_REQUIRED = "AiConsentRequiredToEnableFeature"
    }
}

/**
 * PT21.IA «Activar funciones con IA»: otorga el consentimiento de IA (CR-2) sobre el vínculo activo y devuelve las
 * preferencias como quedaron. Sin vínculo activo → `NotFound`.
 */
class GrantAiProcessingConsentUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
    private val aiPreferencesRepository: AiPreferencesRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<AiPreferences> {
        val patientId = PatientId(patientUserId)
        val link = careLinkRepository.getActiveCareLink(patientId)
        val careLink = link.getOrNull() ?: return domainFailure(link.domainErrorOrNull() ?: DomainError.Unexpected())
        val granted = careLinkRepository.changeAiProcessingConsent(careLink.id, granted = true)
        granted.domainErrorOrNull()?.let { return domainFailure(it) }
        // La lectura puede caer en la copia del teléfono (sin red justo después): el consentimiento ya se otorgó.
        return Result.success(aiPreferencesRepository.getAiPreferences(patientId).copy(consentGranted = true))
    }
}
