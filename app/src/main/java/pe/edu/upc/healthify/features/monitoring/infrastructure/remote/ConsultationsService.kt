package pe.edu.upc.healthify.features.monitoring.infrastructure.remote

import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ConsultationsOverviewDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.PreVisitCheckInDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SubmitCheckInRequestDto
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.SuggestedQuestionsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** The patient's visits, pre-visit check-in and suggested questions. */
interface ConsultationsService {

    /** `403 AccessNotAllowed` if it is not the patient themselves. */
    @GET("patients/{patientId}/consultations-overview")
    suspend fun getOverview(@Path("patientId") patientId: Long): ConsultationsOverviewDto

    /** `404 PreVisitCheckInNotFound` while there is no reply · `404 ScheduledFollowUpNotFound`. */
    @GET("scheduled-follow-ups/{followUpId}/check-in")
    suspend fun getCheckIn(@Path("followUpId") followUpId: Long): PreVisitCheckInDto

    /** `400 FeelingRequired`/`TooManyQuestions`/… · `404` · `409 CheckInLocked`/`FollowUpNotScheduled`. */
    @PUT("scheduled-follow-ups/{followUpId}/check-in")
    suspend fun submitCheckIn(
        @Path("followUpId") followUpId: Long,
        @Body body: SubmitCheckInRequestDto,
    ): PreVisitCheckInDto

    /** `403` feature off · `404 NotEnoughData` · `429` · `502` · `503 AiFeatureDisabled`/`AiProviderUnavailable`. */
    @GET("patients/{patientId}/suggested-questions")
    suspend fun getSuggestedQuestions(
        @Path("patientId") patientId: Long,
        @Query("followUpId") followUpId: Long?,
    ): SuggestedQuestionsDto
}
