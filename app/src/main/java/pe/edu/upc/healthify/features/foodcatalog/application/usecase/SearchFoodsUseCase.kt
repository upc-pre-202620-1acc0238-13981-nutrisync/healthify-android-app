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
 * PT9 · Buscar alimento, local-first (*Search Falls Back To Local Cache Offline*): emite primero lo que encuentra el
 * catálogo guardado en el teléfono y después la lista completada con el servidor. Sin conexión la segunda emisión
 * queda con `localOnly = true` (PT10 «Estás sin conexión: solo buscamos en tu catálogo guardado»).
 *
 * Un error del servidor que no es de red (5xx) también se degrada a lo local: la búsqueda nunca bloquea el registro.
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
