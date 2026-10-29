package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/**
 * Called once the "Something doesn't add up" card has **been drawn** on the home screen.
 * `409 ConsistencyPromptNotIssued` (no prompt to acknowledge, e.g. the index went back to `Normal`) is not an error
 * for the patient: it counts as done.
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
