package pe.edu.upc.healthify.features.monitoring.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.WeeklySummaryRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.WeeklySummaryService
import javax.inject.Inject

class WeeklySummaryRepositoryImpl @Inject constructor(
    private val service: WeeklySummaryService,
) : WeeklySummaryRepository {

    override suspend fun getLatest(patientId: PatientId): Result<WeeklySummaryAvailability> {
        val result = apiCall { service.getLatest(patientId.value) }
        val dto = result.getOrNull()
        if (dto != null) {
            val summary = dto.toDomainOrNull() ?: return domainFailure(DomainError.Unexpected(CODE_INVALID_RESOURCE))
            return Result.success(WeeklySummaryAvailability.Ready(summary))
        }
        val error = result.domainErrorOrNull() ?: DomainError.Unexpected()
        return when {
            // 404 NotEnoughData (con o sin código): todavía no hay resumen.
            error is DomainError.NotFound -> Result.success(WeeklySummaryAvailability.NotYet)
            // 403: función apagada o sin consentimiento (con AiConsentRequired o el Forbid() vacío).
            error is DomainError.Forbidden -> Result.success(WeeklySummaryAvailability.Off)
            error is DomainError.Unexpected && error.code == CODE_AI_FEATURE_DISABLED ->
                Result.success(WeeklySummaryAvailability.Off)
            else -> domainFailure(error)
        }
    }

    private companion object {
        const val CODE_AI_FEATURE_DISABLED = "AiFeatureDisabled"
        const val CODE_INVALID_RESOURCE = "INVALID_WEEKLY_SUMMARY"
    }
}
