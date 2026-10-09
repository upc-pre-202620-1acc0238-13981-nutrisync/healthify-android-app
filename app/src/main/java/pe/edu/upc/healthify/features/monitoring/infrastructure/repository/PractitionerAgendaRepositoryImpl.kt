package pe.edu.upc.healthify.features.monitoring.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerAgendaRepository
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.rescheduleDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toIsoOffset
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PractitionerAgendaService
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.CancelFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduledFollowUpDto
import retrofit2.HttpException
import java.time.Instant
import javax.inject.Inject

class PractitionerAgendaRepositoryImpl @Inject constructor(
    private val service: PractitionerAgendaService,
) : PractitionerAgendaRepository {

    override suspend fun getAgenda(practitionerId: Long, state: FollowUpState?, from: Instant?): Result<List<AgendaVisit>> =
        apiCall { service.getAgenda(practitionerId, state?.code, from?.toIsoOffset()) }
            .map { visits -> visits.mapNotNull { it.toDomainOrNull() } }

    override suspend fun schedule(followUp: NewFollowUp): Result<AgendaVisit> =
        apiCall { service.schedule(followUp.toDto()) }.toVisit()

    override suspend fun cancel(followUpId: Long, reason: CancellationReason?): Result<Unit> = apiCall {
        val response = service.cancel(followUpId, CancelFollowUpRequestDto(reason?.text))
        if (!response.isSuccessful) throw HttpException(response)
    }

    override suspend fun reschedule(
        followUpId: Long,
        scheduledFor: Instant,
        preparation: FollowUpPreparation,
    ): Result<AgendaVisit> = apiCall { service.reschedule(followUpId, rescheduleDto(scheduledFor, preparation)) }.toVisit()

    override suspend fun recordReferral(referral: NewReferral): Result<Unit> = apiCall {
        service.recordReferral(referral.toDto()).close()
    }

    private fun Result<ScheduledFollowUpDto>.toVisit(): Result<AgendaVisit> = fold(
        onSuccess = { dto ->
            dto.toDomainOrNull()?.let { Result.success(it) } ?: domainFailure(DomainError.Unexpected(MALFORMED_RESPONSE))
        },
        onFailure = { Result.failure(it) },
    )

    private companion object {
        const val MALFORMED_RESPONSE = "MALFORMED_RESPONSE"
    }
}
