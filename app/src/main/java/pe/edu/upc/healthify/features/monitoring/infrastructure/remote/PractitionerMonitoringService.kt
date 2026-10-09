package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.MonitoringSummaryDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientMonitoringPanelDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Seguimiento de un paciente para su nutricionista (RM-3, IA-5). */
interface PractitionerMonitoringService {

    @GET("patients/{patientId}/monitoring-panel")
    suspend fun getPanel(
        @Path("patientId") patientId: Long,
        @Query("date") date: String,
        @Query("days") days: Int,
    ): PatientMonitoringPanelDto

    /** Sin rango: los 7 días hasta ayer. */
    @GET("patients/{patientId}/monitoring-summary")
    suspend fun getSummary(@Path("patientId") patientId: Long): MonitoringSummaryDto
}
