package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.repository.SelfWeighInRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import javax.inject.Inject

/** PT12 · guarda el autopesaje (o lo encola sin conexión). */
class RecordSelfWeighInUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    suspend operator fun invoke(patientUserId: Long, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome> =
        repository.record(PatientId(patientUserId), weighIn)
}

/** PT13 · la tendencia de las últimas [weeks] semanas (4 por defecto, como el backend). */
class GetWeightTrendUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    suspend operator fun invoke(patientUserId: Long, weeks: Int = DEFAULT_WEEKS): Result<WeightTrend?> {
        require(weeks in MIN_WEEKS..MAX_WEEKS) { "weeks must be between $MIN_WEEKS and $MAX_WEEKS" }
        return repository.getWeightTrend(PatientId(patientUserId), weeks)
    }

    companion object {
        const val DEFAULT_WEEKS = 4
        const val MIN_WEEKS = 1
        const val MAX_WEEKS = 52
    }
}

/** PT19 · los autopesajes que esperan en la cola del teléfono. */
class ObservePendingSelfWeighInsUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<List<PendingSelfWeighIn>> =
        repository.observePending(PatientId(patientUserId))
}

/** PT13 · el backend aceptó autopesajes de la cola: hay que volver a leer la tendencia. */
class ObserveSyncedSelfWeighInsUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.syncedWeighIns
}

/** PT13.1 · el Snackbar del último autopesaje. */
class ObserveSelfWeighInSavedNoticeUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke(): Flow<SelfWeighInOutcome?> = repository.savedNotice
}

class ConsumeSelfWeighInSavedNoticeUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke() = repository.consumeSavedNotice()
}
