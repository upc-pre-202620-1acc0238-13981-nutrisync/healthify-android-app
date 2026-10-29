package pe.edu.upc.healthify.features.main.infrastructure.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Copy of the patient's data (ZIP of CSV files). */
interface DataExportService {

    /** `200 application/zip` · `403 AccessNotAllowed` for anyone but the patient. `@Streaming`: never held in memory. */
    @Streaming
    @GET("patients/{patientId}/data-export")
    suspend fun download(@Path("patientId") patientId: Long): ResponseBody
}
