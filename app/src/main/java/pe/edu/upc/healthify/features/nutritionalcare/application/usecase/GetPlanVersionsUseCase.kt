package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.newestFirst
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PlanVersionRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import javax.inject.Inject

/** PT4.1 · Versiones del plan, la más reciente primero. Solo de consulta. */
class GetPlanVersionsUseCase @Inject constructor(
    private val repository: PlanVersionRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<List<PlanVersion>> =
        repository.getPlanVersions(PatientId(patientUserId)).map { it.newestFirst() }
}
