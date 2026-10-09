package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.WeeklySummaryAvailability
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId

/** Resumen semanal con IA (IA-2). No se guarda en el teléfono: sin conexión no hay card. */
interface WeeklySummaryRepository {

    /** `GET /patients/{pid}/weekly-summaries/latest`. Sin red o 5xx (salvo `AiFeatureDisabled`) → error. */
    suspend fun getLatest(patientId: PatientId): Result<WeeklySummaryAvailability>
}
