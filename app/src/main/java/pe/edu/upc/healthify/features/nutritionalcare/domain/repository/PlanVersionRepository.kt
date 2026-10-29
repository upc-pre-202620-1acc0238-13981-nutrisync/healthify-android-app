package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId

/** The only NutritionalCare endpoint open to the patient. */
interface PlanVersionRepository {

    /** `GET /patients/{patientId}/plan-versions`: empty if no plan has been published yet. */
    suspend fun getPlanVersions(patientId: PatientId): Result<List<PlanVersion>>
}
