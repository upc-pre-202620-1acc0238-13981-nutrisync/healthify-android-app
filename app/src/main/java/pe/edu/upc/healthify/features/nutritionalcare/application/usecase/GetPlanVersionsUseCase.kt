package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.newestFirst
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PlanVersionRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import javax.inject.Inject

/** Plan versions, newest first. Read-only. */
class GetPlanVersionsUseCase @Inject constructor(
    private val repository: PlanVersionRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<List<PlanVersion>> =
        repository.getPlanVersions(PatientId(patientUserId)).map { it.newestFirst() }
}
