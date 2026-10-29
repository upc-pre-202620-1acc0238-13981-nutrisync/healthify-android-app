package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.AcceptedProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ReviewInboxRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import javax.inject.Inject

/** The open inbox items (`?state=Open`). */
class GetOpenReviewItemsUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(): Result<List<ReviewItem>> = repository.getItems(ReviewItemState.OPEN)
}

/**
 * One open item. There is no `GET /review-items/{id}`: it is looked up in the open inbox; `null` if it is gone
 * (resolved or nonexistent → "This item is no longer available.").
 */
class GetOpenReviewItemUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long): Result<ReviewItem?> =
        repository.getItems(ReviewItemState.OPEN).map { items -> items.firstOrNull { it.id.value == id } }
}

/** "Resolve": requires saying whether the plan was adjusted (the rule lives in [ReviewResolution]). */
class ResolveReviewItemUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long, resolution: ReviewResolution): Result<ReviewItem> =
        repository.resolve(ReviewItemId(id), resolution)
}

/**
 * Waits for the AI proposal: emits [PlanProposalLookup.Generating] while the backend answers `202` and
 * retries after `Retry-After`, up to [maxAttempts] reads. Ends with the proposal, with [PlanProposalLookup.None]
 * (`404`, or when attempts run out: the item opens without AI) or with the network/server failure.
 */
class AwaitPlanProposalUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    operator fun invoke(id: Long, maxAttempts: Int = DEFAULT_MAX_ATTEMPTS): Flow<Result<PlanProposalLookup>> = flow {
        require(maxAttempts > 0) { "At least one attempt" }
        val itemId = ReviewItemId(id)
        repeat(maxAttempts) { attempt ->
            val result = repository.getPlanProposal(itemId)
            val lookup = result.getOrNull()
            if (lookup !is PlanProposalLookup.Generating) {
                emit(result)
                return@flow
            }
            emit(result)
            if (attempt < maxAttempts - 1) delay(lookup.retryAfterSeconds * MILLIS_PER_SECOND)
        }
        // If it is still generating after ~30 s, the item opens without AI (it can be resolved all the same).
        emit(Result.success(PlanProposalLookup.None))
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 6
        private const val MILLIS_PER_SECOND = 1_000L
    }
}

/** "Resolve" (as is) and "Assign adjusted plan" (with edits). */
class AcceptPlanProposalUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long, acceptance: PlanAcceptance): Result<AcceptedProposal> =
        repository.acceptPlanProposal(ReviewItemId(id), acceptance)
}
