package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.AcceptedProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ReviewInboxRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ReviewItemService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanAdjustmentProposalDto
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject

class ReviewInboxRepositoryImpl @Inject constructor(
    private val service: ReviewItemService,
    private val json: Json,
) : ReviewInboxRepository {

    override suspend fun getItems(state: ReviewItemState): Result<List<ReviewItem>> =
        apiCall { service.getItems(state.code) }.mapResource { items -> items.map { it.toDomain() } }

    override suspend fun resolve(id: ReviewItemId, resolution: ReviewResolution): Result<ReviewItem> =
        apiCall { service.resolve(id.value, resolution.toDto()) }.mapResource { it.toDomain() }

    override suspend fun getPlanProposal(id: ReviewItemId): Result<PlanProposalLookup> {
        val result = apiCall {
            val response = service.getPlanProposal(id.value)
            if (!response.isSuccessful) throw HttpException(response)
            if (response.code() == HttpURLConnection.HTTP_ACCEPTED) {
                null to (response.headers()[RETRY_AFTER]?.trim()?.toIntOrNull()?.takeIf { it > 0 }
                    ?: DEFAULT_RETRY_AFTER_SECONDS)
            } else {
                response.body() to 0
            }
        }
        // 404 PlanProposalNotFound = el ítem no tiene propuesta: PR14 sin IA. Otro 404 (el ítem ya no está) es un error.
        val error = result.domainErrorOrNull()
        if (error is DomainError.NotFound && error.code == PLAN_PROPOSAL_NOT_FOUND) {
            return Result.success(PlanProposalLookup.None)
        }
        return result.mapResource { (body, retryAfter) ->
            if (body == null && retryAfter > 0) {
                PlanProposalLookup.Generating(retryAfter)
            } else {
                val element = body ?: throw SerializationException("Empty proposal")
                PlanProposalLookup.Ready(json.decodeFromJsonElement(PlanAdjustmentProposalDto.serializer(), element).toDomain())
            }
        }
    }

    override suspend fun acceptPlanProposal(id: ReviewItemId, acceptance: PlanAcceptance): Result<AcceptedProposal> =
        apiCall { service.acceptPlanProposal(id.value, acceptance.toDto()) }
            .mapResource { AcceptedProposal(it.reviewItem.toDomain(), it.planVersion.version) }

    private companion object {
        const val RETRY_AFTER = "Retry-After"
        const val DEFAULT_RETRY_AFTER_SECONDS = 5
        const val PLAN_PROPOSAL_NOT_FOUND = "PlanProposalNotFound"
    }
}
