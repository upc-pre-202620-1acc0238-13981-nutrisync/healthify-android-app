package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import okhttp3.ResponseBody
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.CancelFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.RecordReferralRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.RescheduleFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduleFollowUpRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ScheduledFollowUpDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The nutritionist's agenda and referrals. */
interface PractitionerAgendaService {

    @GET("practitioners/{practitionerId}/scheduled-follow-ups")
    suspend fun getAgenda(
        @Path("practitionerId") practitionerId: Long,
        @Query("state") state: String?,
        @Query("from") from: String?,
    ): List<ScheduledFollowUpDto>

    @POST("scheduled-follow-ups")
    suspend fun schedule(@Body body: ScheduleFollowUpRequestDto): ScheduledFollowUpDto

    /** `204 No Content`. */
    @POST("scheduled-follow-ups/{followUpId}/cancellation")
    suspend fun cancel(@Path("followUpId") followUpId: Long, @Body body: CancelFollowUpRequestDto): Response<Unit>

    @POST("scheduled-follow-ups/{followUpId}/rescheduling")
    suspend fun reschedule(
        @Path("followUpId") followUpId: Long,
        @Body body: RescheduleFollowUpRequestDto,
    ): ScheduledFollowUpDto

    /** `201` with the `ReferralResource` (unused: the patient profile is re-read on return). */
    @POST("referrals")
    suspend fun recordReferral(@Body body: RecordReferralRequestDto): ResponseBody
}
