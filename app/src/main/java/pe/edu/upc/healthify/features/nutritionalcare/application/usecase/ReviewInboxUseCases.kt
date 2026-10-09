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

/** PR13 · los ítems abiertos de la bandeja (NC-11: `?state=Open`). */
class GetOpenReviewItemsUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(): Result<List<ReviewItem>> = repository.getItems(ReviewItemState.OPEN)
}

/**
 * PR14 / PR14.IA · un ítem abierto. No hay `GET /review-items/{id}`: se busca en la bandeja abierta; `null` si ya no
 * está (resuelto o inexistente → «Este ítem ya no está disponible.»).
 */
class GetOpenReviewItemUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long): Result<ReviewItem?> =
        repository.getItems(ReviewItemState.OPEN).map { items -> items.firstOrNull { it.id.value == id } }
}

/** PR14 «Resolver»: exige decir si el plan se ajustó (la regla vive en [ReviewResolution]). */
class ResolveReviewItemUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long, resolution: ReviewResolution): Result<ReviewItem> =
        repository.resolve(ReviewItemId(id), resolution)
}

/**
 * PR14.IA · espera la propuesta de la IA: emite [PlanProposalLookup.Generating] mientras el backend responde `202` y
 * reintenta después de `Retry-After`, hasta [maxAttempts] lecturas. Termina con la propuesta, con
 * [PlanProposalLookup.None] (`404`, o si se agotan los intentos: PR14 sin IA) o con el fallo de red/servidor.
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
        // DECISIÓN PR14.IA: si después de ~30 s sigue generándose, se muestra PR14 sin IA (el ítem se resuelve igual).
        emit(Result.success(PlanProposalLookup.None))
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 6
        private const val MILLIS_PER_SECOND = 1_000L
    }
}

/** PR14.IA «Resolver» (tal cual) y PR14.IA-A «Asignar plan ajustado» (con ediciones). */
class AcceptPlanProposalUseCase @Inject constructor(
    private val repository: ReviewInboxRepository,
) {
    suspend operator fun invoke(id: Long, acceptance: PlanAcceptance): Result<AcceptedProposal> =
        repository.acceptPlanProposal(ReviewItemId(id), acceptance)
}
