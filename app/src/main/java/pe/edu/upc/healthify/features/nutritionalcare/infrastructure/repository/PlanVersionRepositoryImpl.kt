package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository

import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PlanVersionRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PlanVersionService
import javax.inject.Inject

class PlanVersionRepositoryImpl @Inject constructor(
    private val service: PlanVersionService,
) : PlanVersionRepository {

    override suspend fun getPlanVersions(patientId: PatientId): Result<List<PlanVersion>> =
        apiCall { service.getPlanVersions(patientId.value) }.map { list -> list.mapNotNull { it.toDomainOrNull() } }
}
