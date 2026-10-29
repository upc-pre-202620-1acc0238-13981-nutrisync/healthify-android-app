package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The panel for the last 7 days up to today (the phone's day). */
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

/** The AI summary (requested in parallel with the panel). */
class GetMonitoringSummaryUseCase @Inject constructor(
    private val repository: PractitionerMonitoringRepository,
) {
    suspend operator fun invoke(patientId: Long): Result<MonitoringSummary> = repository.getSummary(PatientId(patientId))
}
