package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** AI weekly summary. Not stored on the phone: offline there is no card. */
interface WeeklySummaryRepository {

    /** `GET /patients/{pid}/weekly-summaries/latest`. Offline or 5xx (except `AiFeatureDisabled`) -> error. */
    suspend fun getLatest(patientId: PatientId): Result<WeeklySummaryAvailability>
}
