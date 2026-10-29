package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote

import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AcknowledgeActiveTargetsRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.CareLinkDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.ChangeAiProcessingConsentRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.GrantConsentRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.RedeemInvitationRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.TargetsReadStatusDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Patient-side care link. */
interface CareLinkService {

    /** The patient's **consented** care link; `404 CareLinkNotFound` if there is none. */
    @GET("patients/{patientId}/care-links/active")
    suspend fun getActiveCareLink(@Path("patientId") patientId: Long): CareLinkDto

    /** A care link by id, even inactive or closed (both participants can read it). */
    @GET("care-links/{careLinkId}")
    suspend fun getCareLink(@Path("careLinkId") careLinkId: Long): CareLinkDto

    /** `201` with the new care link, still without consent. */
    @POST("invitations/redemption")
    suspend fun redeemInvitation(@Body body: RedeemInvitationRequestDto): CareLinkDto

    /** `200` with the now active care link. */
    @POST("care-links/{careLinkId}/consent")
    suspend fun grantConsent(@Path("careLinkId") careLinkId: Long, @Body body: GrantConsentRequestDto): CareLinkDto

    /** `204`. */
    @DELETE("care-links/{careLinkId}/consent")
    suspend fun withdrawConsent(@Path("careLinkId") careLinkId: Long)

    /** `204` · `403` without an active care link · `409` closed care link. Requesting the current state changes nothing. */
    @PUT("care-links/{careLinkId}/ai-processing-consent")
    suspend fun changeAiProcessingConsent(
        @Path("careLinkId") careLinkId: Long,
        @Body body: ChangeAiProcessingConsentRequestDto,
    )

    /** Whether the patient saw the latest published version. */
    @GET("care-links/{careLinkId}/targets-read-status")
    suspend fun getTargetsReadStatus(@Path("careLinkId") careLinkId: Long): TargetsReadStatusDto

    /** `200` with the care link · `422 NoPendingTargetsVersion` if nothing was pending. */
    @POST("care-links/{careLinkId}/targets-acknowledgement")
    suspend fun acknowledgeActiveTargets(
        @Path("careLinkId") careLinkId: Long,
        @Body body: AcknowledgeActiveTargetsRequestDto,
    ): CareLinkDto
}
