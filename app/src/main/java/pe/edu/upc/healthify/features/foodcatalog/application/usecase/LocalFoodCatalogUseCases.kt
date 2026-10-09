package pe.edu.upc.healthify.features.foodcatalog.application.usecase

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import javax.inject.Inject

/**
 * PR15 · busca en el catálogo del servidor (sin la copia offline del paciente). Un texto de menos de 2 caracteres
 * lista el catálogo sin filtro.
 */
class BrowseFoodCatalogUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(query: String): Result<List<ReferenceFood>> =
        repository.browseCatalog(FoodSearchTerm.of(query))
}

/** PR15.1 «Guardar alimento». */
class CreateLocalFoodUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(food: NewLocalFood): Result<ReferenceFood> = repository.createLocalFood(food)
}
