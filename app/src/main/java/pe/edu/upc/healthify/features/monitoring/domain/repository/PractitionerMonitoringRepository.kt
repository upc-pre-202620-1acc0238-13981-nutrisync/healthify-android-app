package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.LocalDate

/** A patient's follow-up as seen by their nutritionist. Not stored on the phone. */
interface PractitionerMonitoringRepository {

    /** `GET /patients/{pid}/monitoring-panel?date=&days=`. `403 AccessNotAllowed` without an active care link. */
    suspend fun getPanel(patientId: PatientId, date: LocalDate, days: Int): Result<PatientMonitoringPanel>

    /** `GET /patients/{pid}/monitoring-summary`: `429`, `502` and `503` are AI failures. */
    suspend fun getSummary(patientId: PatientId): Result<MonitoringSummary>
}
