package pe.edu.upc.healthify.features.monitoring.domain.repository

import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.LocalDate

/** Seguimiento de un paciente visto por su nutricionista (PAC-2). No se guarda en el teléfono. */
interface PractitionerMonitoringRepository {

    /** `GET /patients/{pid}/monitoring-panel?date=&days=` (RM-3). `403 AccessNotAllowed` sin vínculo activo. */
    suspend fun getPanel(patientId: PatientId, date: LocalDate, days: Int): Result<PatientMonitoringPanel>

    /** `GET /patients/{pid}/monitoring-summary` (IA-5): `429`, `502` y `503` son fallos de la IA. */
    suspend fun getSummary(patientId: PatientId): Result<MonitoringSummary>
}
