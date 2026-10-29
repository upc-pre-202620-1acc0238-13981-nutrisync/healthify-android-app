package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.AcceptedProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState

/**
 * Review inbox of the signed-in nutritionist. Not stored on the phone: offline it shows the Figma offline
 * state.
 */
interface ReviewInboxRepository {

    /** `GET /review-items?state=` · the nutritionist's own inbox items, newest first. */
    suspend fun getItems(state: ReviewItemState): Result<List<ReviewItem>>

    /**
     * `POST /review-items/{id}/resolution`. `400 ResolutionOutcomeRequired` without Yes/No, `404 ReviewItemNotFound` if
     * it does not exist or was already resolved. A pending AI proposal is discarded and the plan does not change.
     */
    suspend fun resolve(id: ReviewItemId, resolution: ReviewResolution): Result<ReviewItem>

    /** `GET /review-items/{id}/plan-proposal`: `200` ready, `202` + `Retry-After` generating, `404` no proposal. */
    suspend fun getPlanProposal(id: ReviewItemId): Result<PlanProposalLookup>

    /** `POST /review-items/{id}/plan-proposal/acceptance`: creates the adjusted version and resolves the item at once. */
    suspend fun acceptPlanProposal(id: ReviewItemId, acceptance: PlanAcceptance): Result<AcceptedProposal>
}
