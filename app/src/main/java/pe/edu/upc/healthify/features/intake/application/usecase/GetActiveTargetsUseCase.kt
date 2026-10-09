package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import javax.inject.Inject

/** PT3 / PT4: metas vigentes, desde el backend o, sin conexión, desde la copia del teléfono. */
class GetActiveTargetsUseCase @Inject constructor(
    private val repository: ActiveTargetsRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ActiveTargetsLookup> =
        repository.getActiveTargets(PatientId(patientUserId))
}
