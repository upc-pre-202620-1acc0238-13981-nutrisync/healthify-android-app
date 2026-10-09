package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId

/** El único endpoint de NutritionalCare abierto al paciente (NC-8). */
interface PlanVersionRepository {

    /** `GET /patients/{patientId}/plan-versions`: vacía si todavía no hay plan publicado. */
    suspend fun getPlanVersions(patientId: PatientId): Result<List<PlanVersion>>
}
