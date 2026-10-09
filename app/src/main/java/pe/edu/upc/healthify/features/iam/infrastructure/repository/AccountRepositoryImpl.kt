package pe.edu.upc.healthify.features.iam.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.iam.domain.repository.AccountRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.remote.SessionService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val sessionService: SessionService,
    private val localDataSource: SessionLocalDataSource,
    private val clock: Clock,
) : AccountRepository {

    override suspend fun getCreatedOn(userId: UserId): Result<LocalDate> {
        localDataSource.accountCreatedAt(userId.value)?.toInstantOrNull()?.let { return Result.success(it.toLocalDay()) }
        val user = apiCall { sessionService.getUser(userId.value) }.getOrElse { return Result.failure(it) }
        val raw = user.createdAt
        val createdAt = raw?.toInstantOrNull() ?: return domainFailure(DomainError.Unexpected(MALFORMED_RESPONSE))
        localDataSource.saveAccountCreatedAt(userId.value, raw)
        return Result.success(createdAt.toLocalDay())
    }

    private fun Instant.toLocalDay(): LocalDate = atZone(clock.zone).toLocalDate()

    private companion object {
        const val MALFORMED_RESPONSE = "MALFORMED_RESPONSE"
    }
}
