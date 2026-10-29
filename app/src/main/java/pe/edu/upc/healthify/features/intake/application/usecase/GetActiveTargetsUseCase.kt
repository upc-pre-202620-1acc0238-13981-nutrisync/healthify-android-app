package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.entity.DailyIntake
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Active targets, from the backend or, when offline, from the phone's copy. */
class GetActiveTargetsUseCase @Inject constructor(
    private val repository: ActiveTargetsRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<ActiveTargetsLookup> =
        repository.getActiveTargets(PatientId(patientUserId))
}

/** The daily gauge: what was logged today and what is left of each target. */
class GetDailyIntakeUseCase @Inject constructor(
    private val repository: ActiveTargetsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(patientUserId: Long): Result<DailyIntake> =
        repository.getDailyIntake(PatientId(patientUserId), LocalDate.now(clock))
}
