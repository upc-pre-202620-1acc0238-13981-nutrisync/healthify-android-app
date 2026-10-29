package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId

/** `GET /patients/{patientId}/ai-preferences` with the last read saved on the phone. */
interface AiPreferencesRepository {

    /**
     * Fetches the preferences and saves the read. Offline (or on a 5xx) it returns the phone's last read;
     * if they were never read, [AiPreferences.NoneGranted]. `403` (no care link) also means "none".
     */
    suspend fun getAiPreferences(patientId: PatientId): AiPreferences

    /**
     * `PUT /patients/{patientId}/ai-preferences` with all four preferences; saves the response. `409
     * AiConsentRequiredToEnableFeature` if one is turned on without AI consent.
     */
    suspend fun updateAiPreferences(patientId: PatientId, preferences: AiPreferences): Result<AiPreferences>
}
