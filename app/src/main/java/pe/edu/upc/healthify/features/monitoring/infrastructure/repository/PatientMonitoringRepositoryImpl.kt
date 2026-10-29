package pe.edu.upc.healthify.features.monitoring.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.ConsistencyIndex
import pe.edu.upc.healthify.features.monitoring.domain.entity.DailyProgress
import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDailyProgress
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PatientMonitoringService
import java.time.LocalDate
import javax.inject.Inject

class PatientMonitoringRepositoryImpl @Inject constructor(
    private val service: PatientMonitoringService,
) : PatientMonitoringRepository {

    override suspend fun getDailyProgress(patientId: PatientId, date: LocalDate): Result<DailyProgress> =
        apiCall { service.getDailyCompliance(patientId.value, date.toString()) }.map { it.toDailyProgress(date) }

    override suspend fun getComplianceSummary(
        patientId: PatientId,
        from: LocalDate,
        to: LocalDate,
    ): Result<ComplianceSummary> {
        val result = apiCall { service.getDailyComplianceRange(patientId.value, from.toString(), to.toString()) }
        return result.mapCatching { it.summary.toDomain() }.invalidAsUnexpected(result)
    }

    override suspend fun getComplianceDays(
        patientId: PatientId,
        from: LocalDate,
        to: LocalDate,
    ): Result<List<ComplianceDay>> =
        apiCall { service.getDailyComplianceRange(patientId.value, from.toString(), to.toString()) }
            .map { range -> range.days.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getConsistencyIndex(patientId: PatientId): Result<ConsistencyIndex?> {
        val result = apiCall { service.getConsistencyIndex(patientId.value) }
        val error = result.domainErrorOrNull()
        // "No index yet" is not an error for the patient; there is simply no card.
        if (error is DomainError.Validation && error.code == CODE_BOTH_SERIES_REQUIRED) return Result.success(null)
        return result.map { it.toDomain() }
    }

    override suspend fun acknowledgeConsistencyPrompt(patientId: PatientId): Result<Unit> =
        apiCall { service.acknowledgeConsistencyPrompt(patientId.value) }

    override suspend fun getNextFollowUp(patientId: PatientId): Result<NextFollowUp?> {
        val result = apiCall { service.getNextFollowUp(patientId.value) }
        if (result.domainErrorOrNull() is DomainError.NotFound) return Result.success(null)
        return result.mapCatching<NextFollowUp?, _> { it.toDomain() }.invalidAsUnexpected(result)
    }

    /** A resource that breaks a domain invariant is reported as an unexpected response, not as an exception. */
    private fun <T> Result<T>.invalidAsUnexpected(original: Result<*>): Result<T> =
        if (isFailure && original.isSuccess) domainFailure(DomainError.Unexpected(CODE_INVALID_RESOURCE)) else this

    private companion object {
        const val CODE_BOTH_SERIES_REQUIRED = "BothSeriesRequired"
        const val CODE_INVALID_RESOURCE = "INVALID_MONITORING_RESOURCE"
    }
}
