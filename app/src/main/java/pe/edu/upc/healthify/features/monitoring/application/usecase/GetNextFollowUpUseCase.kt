package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId
import javax.inject.Inject

/** PT3 «Próxima consulta»: `null` si no hay ninguna agendada (la fila se oculta). */
class GetNextFollowUpUseCase @Inject constructor(
    private val repository: PatientMonitoringRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<NextFollowUp?> =
        repository.getNextFollowUp(PatientId(patientUserId))
}
