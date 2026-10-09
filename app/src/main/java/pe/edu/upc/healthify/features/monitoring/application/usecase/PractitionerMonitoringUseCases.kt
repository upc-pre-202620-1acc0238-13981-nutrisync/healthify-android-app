package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** PAC-2 · el panel de los últimos 7 días hasta hoy (el día del teléfono). */
class GetMonitoringPanelUseCase @Inject constructor(
    private val repository: PractitionerMonitoringRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(patientId: Long): Result<PatientMonitoringPanel> =
        repository.getPanel(PatientId(patientId), LocalDate.now(clock), PANEL_DAYS)

    private companion object {
        const val PANEL_DAYS = 7
    }
}

/** PAC-2 · el resumen con IA (se pide en paralelo al panel). */
class GetMonitoringSummaryUseCase @Inject constructor(
    private val repository: PractitionerMonitoringRepository,
) {
    suspend operator fun invoke(patientId: Long): Result<MonitoringSummary> = repository.getSummary(PatientId(patientId))
}
