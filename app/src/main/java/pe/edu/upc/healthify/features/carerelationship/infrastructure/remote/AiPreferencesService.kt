package pe.edu.upc.healthify.features.carerelationship.infrastructure.remote

import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AiPreferencesDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.UpdateAiPreferencesRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/** IA-1 · consentimiento de IA y preferencias por función del paciente dueño. */
interface AiPreferencesService {

    @GET("patients/{patientId}/ai-preferences")
    suspend fun getAiPreferences(@Path("patientId") patientId: Long): AiPreferencesDto

    /** `200` con las preferencias como quedaron · `409 AiConsentRequiredToEnableFeature`. */
    @PUT("patients/{patientId}/ai-preferences")
    suspend fun updateAiPreferences(
        @Path("patientId") patientId: Long,
        @Body body: UpdateAiPreferencesRequestDto,
    ): AiPreferencesDto
}
