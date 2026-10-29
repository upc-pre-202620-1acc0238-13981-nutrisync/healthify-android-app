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
 * Turns an AI feature on or off. Turning one on without AI consent never reaches the backend (it would answer
 * `409 AiConsentRequiredToEnableFeature`): it fails the same way on the phone and the UI offers to grant consent.
 * Turning one off is always allowed and changes neither the plan nor the logs.
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
 * "Turn on AI features": grants AI consent on the active care link and returns the preferences as they ended up.
 * No active care link → `NotFound`.
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
        // The read may fall back to the phone's copy (no network right after): consent was already granted.
        return Result.success(aiPreferencesRepository.getAiPreferences(patientId).copy(consentGranted = true))
    }
}
