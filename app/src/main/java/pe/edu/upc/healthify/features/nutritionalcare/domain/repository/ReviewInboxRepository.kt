package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.AcceptedProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState

/**
 * Bandeja de revisión del nutricionista con sesión (F27, F40, NC-10, NC-11). No se guarda en el teléfono: sin conexión
 * se muestra el estado offline del Figma.
 */
interface ReviewInboxRepository {

    /** `GET /review-items?state=` · los ítems de la bandeja propia, más recientes primero. */
    suspend fun getItems(state: ReviewItemState): Result<List<ReviewItem>>

    /**
     * `POST /review-items/{id}/resolution`. `400 ResolutionOutcomeRequired` sin Sí/No, `404 ReviewItemNotFound` si no
     * existe o ya estaba resuelto. Una propuesta de IA pendiente queda descartada y el plan no cambia.
     */
    suspend fun resolve(id: ReviewItemId, resolution: ReviewResolution): Result<ReviewItem>

    /** `GET /review-items/{id}/plan-proposal`: `200` lista, `202` + `Retry-After` generándose, `404` sin propuesta. */
    suspend fun getPlanProposal(id: ReviewItemId): Result<PlanProposalLookup>

    /** `POST /review-items/{id}/plan-proposal/acceptance`: crea la versión ajustada y resuelve el ítem en un solo paso. */
    suspend fun acceptPlanProposal(id: ReviewItemId, acceptance: PlanAcceptance): Result<AcceptedProposal>
}
