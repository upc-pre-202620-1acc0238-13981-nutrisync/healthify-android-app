package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.MonitoringSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientMonitoringPanelDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** A patient's follow-up for their nutritionist. */
interface PractitionerMonitoringService {

    @GET("patients/{patientId}/monitoring-panel")
    suspend fun getPanel(
        @Path("patientId") patientId: Long,
        @Query("date") date: String,
        @Query("days") days: Int,
    ): PatientMonitoringPanelDto

    /** No range: the 7 days up to yesterday. */
    @GET("patients/{patientId}/monitoring-summary")
    suspend fun getSummary(@Path("patientId") patientId: Long): MonitoringSummaryDto
}
