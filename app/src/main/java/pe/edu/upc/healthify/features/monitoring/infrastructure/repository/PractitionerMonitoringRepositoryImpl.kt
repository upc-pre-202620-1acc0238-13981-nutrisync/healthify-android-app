package pe.edu.upc.healthify.features.monitoring.infrastructure.repository

import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PractitionerMonitoringService
import java.time.LocalDate
import javax.inject.Inject

class PractitionerMonitoringRepositoryImpl @Inject constructor(
    private val service: PractitionerMonitoringService,
) : PractitionerMonitoringRepository {

    override suspend fun getPanel(patientId: PatientId, date: LocalDate, days: Int): Result<PatientMonitoringPanel> =
        apiCall { service.getPanel(patientId.value, date.toString(), days).toDomain() }

    override suspend fun getSummary(patientId: PatientId): Result<MonitoringSummary> =
        apiCall { service.getSummary(patientId.value).toDomain() }
}
