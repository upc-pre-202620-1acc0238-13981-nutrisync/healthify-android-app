package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.WeeklySummaryDto
import retrofit2.http.GET
import retrofit2.http.Path

/** IA-2 · resumen semanal del paciente (solo el dueño). */
interface WeeklySummaryService {

    /**
     * `404 NotEnoughData` mientras no hay resumen · `403 AiConsentRequired` (o vacío) con la función apagada ·
     * `503 AiFeatureDisabled` con la IA del servidor apagada.
     */
    @GET("patients/{patientId}/weekly-summaries/latest")
    suspend fun getLatest(@Path("patientId") patientId: Long): WeeklySummaryDto
}
