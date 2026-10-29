package pe.edu.upc.healthify.features.foodcatalog.application.usecase

import pe.edu.upc.healthify.features.foodcatalog.domain.entity.NewLocalFood
import pe.edu.upc.healthify.features.foodcatalog.domain.entity.ReferenceFood
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.domain.valueobject.FoodSearchTerm
import javax.inject.Inject

/**
 * Searches the server catalog (without the patient's offline copy). Text shorter than 2 characters
 * lists the catalog unfiltered.
 */
class BrowseFoodCatalogUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(query: String): Result<List<ReferenceFood>> =
        repository.browseCatalog(FoodSearchTerm.of(query))
}

/** "Save food". */
class CreateLocalFoodUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(food: NewLocalFood): Result<ReferenceFood> = repository.createLocalFood(food)
}
