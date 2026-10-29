package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsistencyIndexDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.DailyComplianceRangeDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PatientFollowUpDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Patient side of MonitoringAdherence. Dates go as `yyyy-MM-dd` (clinical day). */
interface PatientMonitoringService {

    /** List form (no range): the evaluated days for that date, one per patient window. */
    @GET("patients/{patientId}/daily-compliance")
    suspend fun getDailyCompliance(
        @Path("patientId") patientId: Long,
        @Query("date") date: String,
    ): List<DailyComplianceDto>

    /** `400 InvalidComplianceRange` if the range is inverted or longer than 31 days. */
    @GET("patients/{patientId}/daily-compliance")
    suspend fun getDailyComplianceRange(
        @Path("patientId") patientId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
    ): DailyComplianceRangeDto

    /** `422 BothSeriesRequired` while there is no index. */
    @GET("patients/{patientId}/consistency-index")
    suspend fun getConsistencyIndex(@Path("patientId") patientId: Long): ConsistencyIndexDto

    /** `204` · `409 ConsistencyPromptNotIssued` if there was no prompt. */
    @POST("patients/{patientId}/consistency-index/prompt-acknowledgement")
    suspend fun acknowledgeConsistencyPrompt(@Path("patientId") patientId: Long)

    /** `404 ScheduledFollowUpNotFound` if none is scheduled. */
    @GET("patients/{patientId}/scheduled-follow-ups/next")
    suspend fun getNextFollowUp(@Path("patientId") patientId: Long): PatientFollowUpDto
}
