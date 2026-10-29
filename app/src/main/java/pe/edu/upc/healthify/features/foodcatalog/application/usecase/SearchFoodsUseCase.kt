package pe.edu.upc.healthify.features.foodcatalog.application.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.FoodSearchResult
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import javax.inject.Inject

/**
 * Search food, local-first (*Search Falls Back To Local Cache Offline*): first emits what the catalog saved on
 * the phone finds, then the list completed by the server. Offline, the second emission
 * has `localOnly = true` ("You're offline: we only search your saved catalog").
 *
 * A server error that is not a network error (5xx) also falls back to local: search never blocks logging.
 */
class SearchFoodsUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    operator fun invoke(term: FoodSearchTerm): Flow<FoodSearchResult> = flow {
        val local = repository.searchLocal(term)
        emit(FoodSearchResult(term, local, serverPending = true, localOnly = false))
        val remote = repository.searchRemote(term)
        val foods = FoodSearchResult.merge(local, remote.getOrDefault(emptyList()))
        val offline = remote.domainErrorOrNull() == DomainError.Network
        emit(FoodSearchResult(term, foods, serverPending = false, localOnly = offline))
    }
}
