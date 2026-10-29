package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.entity.DailyIntake
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** PT3 / PT4: metas vigentes, desde el backend o, sin conexión, desde la copia del teléfono. */
class GetActiveTargetsUseCase @Inject constructor(
    private val repository: ActiveTargetsRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ActiveTargetsLookup> =
        repository.getActiveTargets(PatientId(patientUserId))
}

/** PT3 «Medidor del día»: lo registrado hoy y lo que queda de cada meta (IN-9). */
class GetDailyIntakeUseCase @Inject constructor(
    private val repository: ActiveTargetsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(patientUserId: Long): Result<DailyIntake> =
        repository.getDailyIntake(PatientId(patientUserId), LocalDate.now(clock))
}
