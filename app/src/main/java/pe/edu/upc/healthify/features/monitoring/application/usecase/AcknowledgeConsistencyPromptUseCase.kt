package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * MA-7: se llama cuando la tarjeta «Algo no cuadra» **se pintó** en PT3. `409 ConsistencyPromptNotIssued` (no había
 * aviso que acusar, p. ej. el índice volvió a `Normal`) no es un error para el paciente: se trata como hecho.
 */
class AcknowledgeConsistencyPromptUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<Unit> {
        val result = repository.acknowledgeConsistencyPrompt(PatientId(patientUserId))
        val error = result.domainErrorOrNull()
        return if (error is DomainError.Conflict && error.code == CODE_PROMPT_NOT_ISSUED) Result.success(Unit) else result
    }

    companion object {
        const val CODE_PROMPT_NOT_ISSUED = "ConsistencyPromptNotIssued"
    }
}
