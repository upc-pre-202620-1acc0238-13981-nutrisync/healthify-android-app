package pe.edu.upc.healthify.features.foodcatalog.application.usecase

import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import javax.inject.Inject

/** Mantiene la copia del catálogo del teléfono (≤ 500 alimentos) para registrar sin conexión (PT9 offline). */
class RefreshLocalFoodCatalogUseCase @Inject constructor(
    private val repository: ReferenceFoodRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<Unit> = repository.refreshLocalCatalog(patientUserId)
}
