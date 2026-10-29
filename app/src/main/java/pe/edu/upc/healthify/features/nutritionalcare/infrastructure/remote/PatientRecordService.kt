package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientOwnRecordDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Patient Record read model. With the patient's token it returns `PatientOwnRecordResource`. */
interface PatientRecordService {

    /** `403 AccessNotAllowed` if it is not the caller's own record. */
    @GET("patients/{patientId}/record")
    suspend fun getOwnRecord(
        @Path("patientId") patientId: Long,
        @Query("days") days: Int,
    ): PatientOwnRecordDto
}
