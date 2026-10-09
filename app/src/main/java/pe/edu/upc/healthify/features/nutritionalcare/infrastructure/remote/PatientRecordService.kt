package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote

import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientOwnRecordDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Read model Patient Record (RM-4). Con el token del paciente devuelve `PatientOwnRecordResource`. */
interface PatientRecordService {

    /** `403 AccessNotAllowed` si no es el propio expediente. */
    @GET("patients/{patientId}/record")
    suspend fun getOwnRecord(
        @Path("patientId") patientId: Long,
        @Query("days") days: Int,
    ): PatientOwnRecordDto
}
