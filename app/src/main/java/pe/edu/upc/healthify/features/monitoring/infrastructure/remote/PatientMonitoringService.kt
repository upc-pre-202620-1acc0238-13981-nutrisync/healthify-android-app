package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsistencyIndexDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceRangeDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientFollowUpDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** MonitoringAdherence del lado del paciente (§5.6). Las fechas van como `yyyy-MM-dd` (día clínico). */
interface PatientMonitoringService {

    /** Forma de lista (sin rango): los días evaluados de esa fecha, de cada ventana del paciente. */
    @GET("patients/{patientId}/daily-compliance")
    suspend fun getDailyCompliance(
        @Path("patientId") patientId: Long,
        @Query("date") date: String,
    ): List<DailyComplianceDto>

    /** MA-6 · `400 InvalidComplianceRange` si el rango está invertido o pasa de 31 días. */
    @GET("patients/{patientId}/daily-compliance")
    suspend fun getDailyComplianceRange(
        @Path("patientId") patientId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
    ): DailyComplianceRangeDto

    /** `422 BothSeriesRequired` mientras no haya índice. */
    @GET("patients/{patientId}/consistency-index")
    suspend fun getConsistencyIndex(@Path("patientId") patientId: Long): ConsistencyIndexDto

    /** MA-7 · `204` · `409 ConsistencyPromptNotIssued` si no había aviso. */
    @POST("patients/{patientId}/consistency-index/prompt-acknowledgement")
    suspend fun acknowledgeConsistencyPrompt(@Path("patientId") patientId: Long)

    /** MA-3 · `404 ScheduledFollowUpNotFound` si no hay ninguna agendada. */
    @GET("patients/{patientId}/scheduled-follow-ups/next")
    suspend fun getNextFollowUp(@Path("patientId") patientId: Long): PatientFollowUpDto
}
