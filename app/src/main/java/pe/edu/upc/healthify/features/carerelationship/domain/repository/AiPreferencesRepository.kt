package pe.edu.upc.healthify.features.carerelationship.domain.repository

import pe.edu.upc.healthify.features.carerelationship.domain.entity.AiPreferences
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId

/** `GET /patients/{patientId}/ai-preferences` (IA-1) con la última lectura guardada en el teléfono. */
interface AiPreferencesRepository {

    /**
     * Pide las preferencias y guarda la lectura. Sin conexión (o con un 5xx) devuelve la última lectura del
     * teléfono; si nunca se leyeron, [AiPreferences.NoneGranted]. `403` (sin vínculo) también es «ninguna».
     */
    suspend fun getAiPreferences(patientId: PatientId): AiPreferences

    /**
     * `PUT /patients/{patientId}/ai-preferences` (IA-1) con las cuatro preferencias; guarda la respuesta. `409
     * AiConsentRequiredToEnableFeature` si se enciende una sin el consentimiento de IA.
     */
    suspend fun updateAiPreferences(patientId: PatientId, preferences: AiPreferences): Result<AiPreferences>
}
