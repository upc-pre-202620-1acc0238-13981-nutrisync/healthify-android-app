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

/** The nutritionist's roster and invitations. */
interface PractitionerCareLinkService {

    @GET("practitioners/{practitionerId}/patient-roster")
    suspend fun getRoster(@Path("practitionerId") practitionerId: Long): List<PatientRosterItemDto>

    /** `201` with the token (the only time it leaves the server). */
    @POST("invitations")
    suspend fun issueInvitation(@Body body: IssueInvitationRequestDto): InvitationDto

    /** Clinical discharge: `200` with the closed `CareLinkResource` (cancels appointments and turns off the link's AI). */
    @POST("care-links/{careLinkId}/discharge")
    suspend fun discharge(@Path("careLinkId") careLinkId: Long, @Body body: DischargePatientRequestDto): CareLinkDto
}
