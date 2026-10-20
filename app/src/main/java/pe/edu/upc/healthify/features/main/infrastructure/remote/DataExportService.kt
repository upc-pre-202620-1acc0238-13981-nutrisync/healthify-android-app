package pe.edu.upc.healthify.features.main.infrastructure.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Copia de los datos del paciente (ZIP de CSV). */
interface DataExportService {

    /** `200 application/zip` · `403 AccessNotAllowed` si no es el propio paciente. `@Streaming`: no se carga en memoria. */
    @Streaming
    @GET("patients/{patientId}/data-export")
    suspend fun download(@Path("patientId") patientId: Long): ResponseBody
}
