package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote

import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.CareLinkDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.DischargePatientRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.InvitationDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.IssueInvitationRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.PatientRosterItemDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Cartera e invitaciones del nutricionista (RM-1, F4). */
interface PractitionerCareLinkService {

    @GET("practitioners/{practitionerId}/patient-roster")
    suspend fun getRoster(@Path("practitionerId") practitionerId: Long): List<PatientRosterItemDto>

    /** `201` con el token (la única vez que sale del servidor). */
    @POST("invitations")
    suspend fun issueInvitation(@Body body: IssueInvitationRequestDto): InvitationDto

    /** CR-4 · alta clínica: `200` con el `CareLinkResource` cerrado (cancela las citas y apaga la IA del vínculo). */
    @POST("care-links/{careLinkId}/discharge")
    suspend fun discharge(@Path("careLinkId") careLinkId: Long, @Body body: DischargePatientRequestDto): CareLinkDto
}
