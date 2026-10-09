package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT23 · Retirar el consentimiento del vínculo activo (F28). Sin justificación (*No Justification Required*).
 *
 * Sin consentimiento vigente falla con `Forbidden(NoActiveConsent)` y el dispositivo deja de considerar activo el
 * vínculo (la pantalla sigue a PT24 igual). DECISIÓN PT23: si `/care-links/active` responde 404 no hay ningún
 * vínculo con consentimiento, que es la misma situación que `NoActiveConsent`.
 */
class WithdrawConsentUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<Unit> {
        val patientId = PatientId(patientUserId)
        val active = careLinkRepository.getActiveCareLink(patientId)
        val link = active.getOrElse {
            return if (active.domainErrorOrNull() is DomainError.NotFound) noActiveConsent(patientId) else Result.failure(it)
        }
        val result = careLinkRepository.withdrawConsent(patientId, link.id)
        val error = result.domainErrorOrNull()
        return if (error is DomainError.Forbidden && error.code == CODE_NO_ACTIVE_CONSENT) {
            noActiveConsent(patientId)
        } else {
            result
        }
    }

    private suspend fun noActiveConsent(patientId: PatientId): Result<Unit> {
        careLinkRepository.markLinkClosed(patientId)
        return domainFailure(DomainError.Forbidden(CODE_NO_ACTIVE_CONSENT))
    }

    companion object {
        const val CODE_NO_ACTIVE_CONSENT = "NoActiveConsent"
    }
}
