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

/** Vínculo asistencial del lado del paciente (§5.2). */
interface CareLinkService {

    /** Vínculo **con consentimiento** del paciente; `404 CareLinkNotFound` si no hay ninguno. */
    @GET("patients/{patientId}/care-links/active")
    suspend fun getActiveCareLink(@Path("patientId") patientId: Long): CareLinkDto

    /** Un vínculo por id, también inactivo o cerrado (lo leen sus dos participantes). */
    @GET("care-links/{careLinkId}")
    suspend fun getCareLink(@Path("careLinkId") careLinkId: Long): CareLinkDto

    /** F5 · `201` con el vínculo nuevo, todavía sin consentimiento. */
    @POST("invitations/redemption")
    suspend fun redeemInvitation(@Body body: RedeemInvitationRequestDto): CareLinkDto

    /** F6 · `200` con el vínculo ya activo. */
    @POST("care-links/{careLinkId}/consent")
    suspend fun grantConsent(@Path("careLinkId") careLinkId: Long, @Body body: GrantConsentRequestDto): CareLinkDto

    /** F28 · `204`. */
    @DELETE("care-links/{careLinkId}/consent")
    suspend fun withdrawConsent(@Path("careLinkId") careLinkId: Long)

    /** CR-2 · `204` · `403` sin vínculo activo · `409` vínculo cerrado. Pedir el estado actual no cambia nada. */
    @PUT("care-links/{careLinkId}/ai-processing-consent")
    suspend fun changeAiProcessingConsent(
        @Path("careLinkId") careLinkId: Long,
        @Body body: ChangeAiProcessingConsentRequestDto,
    )

    /** F12 / CR-3 · si el paciente vio la última versión publicada. */
    @GET("care-links/{careLinkId}/targets-read-status")
    suspend fun getTargetsReadStatus(@Path("careLinkId") careLinkId: Long): TargetsReadStatusDto

    /** F12 · `200` con el vínculo · `422 NoPendingTargetsVersion` si no había nada pendiente. */
    @POST("care-links/{careLinkId}/targets-acknowledgement")
    suspend fun acknowledgeActiveTargets(
        @Path("careLinkId") careLinkId: Long,
        @Body body: AcknowledgeActiveTargetsRequestDto,
    ): CareLinkDto
}
