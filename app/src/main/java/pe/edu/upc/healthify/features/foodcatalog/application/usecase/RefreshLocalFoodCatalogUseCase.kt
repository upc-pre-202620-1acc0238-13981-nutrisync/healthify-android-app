package pe.edu.upc.healthify.features.foodcatalog.application.usecase

import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import javax.inject.Inject

/** Keeps the phone's copy of the catalog (≤ 500 foods) so meals can be logged offline. */
class RefreshLocalFoodCatalogUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<Unit> = repository.refreshLocalCatalog(patientUserId)
}
