package pe.edu.upc.healthify.features.monitoring.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.monitoring.domain.entity.CheckInAnswer
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsultationsOverview
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpId
import pe.edu.upc.healthify.features.monitoring.domain.entity.PreVisitCheckIn
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestionsAvailability
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationsRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.ConsultationsService
import javax.inject.Inject

class ConsultationsRepositoryImpl @Inject constructor(
    private val service: ConsultationsService,
) : ConsultationsRepository {

    override suspend fun getOverview(patientId: PatientId): Result<ConsultationsOverview> =
        apiCall { service.getOverview(patientId.value) }.map { it.toDomain() }

    override suspend fun getCheckIn(followUpId: FollowUpId): Result<PreVisitCheckIn?> {
        val result = apiCall { service.getCheckIn(followUpId.value) }
        result.getOrNull()?.let { dto ->
            return dto.toDomainOrNull()?.let { Result.success(it) }
                ?: domainFailure(DomainError.Unexpected(CODE_INVALID_CHECK_IN))
        }
        val error = result.domainErrorOrNull() ?: DomainError.Unexpected()
        // 404 PreVisitCheckInNotFound: todavía sin responder. Cualquier otro 404 (la consulta no existe) es error.
        return if (error is DomainError.NotFound && error.code == CODE_CHECK_IN_NOT_FOUND) {
            Result.success(null)
        } else {
            domainFailure(error)
        }
    }

    override suspend fun submitCheckIn(followUpId: FollowUpId, answer: CheckInAnswer): Result<PreVisitCheckIn> {
        val result = apiCall { service.submitCheckIn(followUpId.value, answer.toDto()) }
        val dto = result.getOrNull() ?: return domainFailure(result.domainErrorOrNull() ?: DomainError.Unexpected())
        return dto.toDomainOrNull()?.let { Result.success(it) }
            ?: domainFailure(DomainError.Unexpected(CODE_INVALID_CHECK_IN))
    }

    override suspend fun getSuggestedQuestions(
        patientId: PatientId,
        followUpId: FollowUpId?,
    ): Result<SuggestedQuestionsAvailability> {
        val result = apiCall { service.getSuggestedQuestions(patientId.value, followUpId?.value) }
        result.getOrNull()?.let { dto ->
            val suggestions = dto.toDomain()
            return Result.success(
                if (suggestions.questions.isEmpty()) {
                    SuggestedQuestionsAvailability.NotYet
                } else {
                    SuggestedQuestionsAvailability.Ready(suggestions)
                },
            )
        }
        val error = result.domainErrorOrNull() ?: DomainError.Unexpected()
        return when {
            // 404 NotEnoughData (menos de 3 días registrados): todavía no hay preguntas.
            error is DomainError.NotFound -> Result.success(SuggestedQuestionsAvailability.NotYet)
            // 403: función apagada o sin consentimiento de IA (con código o el Forbid() vacío).
            error is DomainError.Forbidden -> Result.success(SuggestedQuestionsAvailability.Off)
            error is DomainError.Unexpected && error.code == CODE_AI_FEATURE_DISABLED ->
                Result.success(SuggestedQuestionsAvailability.Off)
            else -> domainFailure(error)
        }
    }

    private companion object {
        const val CODE_CHECK_IN_NOT_FOUND = "PreVisitCheckInNotFound"
        const val CODE_AI_FEATURE_DISABLED = "AiFeatureDisabled"
        const val CODE_INVALID_CHECK_IN = "INVALID_CHECK_IN"
    }
}
