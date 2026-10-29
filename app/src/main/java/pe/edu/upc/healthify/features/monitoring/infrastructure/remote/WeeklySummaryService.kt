package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.WeeklySummaryDto
import retrofit2.http.GET
import retrofit2.http.Path

/** The patient's weekly summary (owner only). */
interface WeeklySummaryService {

    /**
     * `404 NotEnoughData` while there is no summary · `403 AiConsentRequired` (or empty) with the feature off ·
     * `503 AiFeatureDisabled` with the server's AI off.
     */
    @GET("patients/{patientId}/weekly-summaries/latest")
    suspend fun getLatest(@Path("patientId") patientId: Long): WeeklySummaryDto
}
