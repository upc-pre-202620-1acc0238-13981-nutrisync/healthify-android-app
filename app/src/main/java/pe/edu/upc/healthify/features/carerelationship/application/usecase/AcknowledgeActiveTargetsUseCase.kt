package pe.edu.upc.healthify.features.carerelationship.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * PT4 «Ya las revisé» (F12): acusa recibo de [planVersion] en el vínculo activo. Acusar no cambia el plan.
 *
 * `422 NoPendingTargetsVersion` (ya no había nada pendiente, p. ej. otro dispositivo ya acusó) se trata como éxito:
 * el resultado que el paciente buscaba ya está.
 */
class AcknowledgeActiveTargetsUseCase @Inject constructor(
    private val careLinkRepository: CareLinkRepository,
) {
    suspend operator fun invoke(patientUserId: Long, planVersion: Int): Result<Unit> {
        require(planVersion > 0) { "Plan versions are positive" }
        val link = careLinkRepository.getActiveCareLink(PatientId(patientUserId))
            .getOrElse { return Result.failure(it) }
        val result = careLinkRepository.acknowledgeActiveTargets(link.id, planVersion)
        val error = result.domainErrorOrNull()
        return if (error is DomainError.Validation && error.code == CODE_NO_PENDING_VERSION) {
            Result.success(Unit)
        } else {
            result
        }
    }

    companion object {
        const val CODE_NO_PENDING_VERSION = "NoPendingTargetsVersion"
    }
}
