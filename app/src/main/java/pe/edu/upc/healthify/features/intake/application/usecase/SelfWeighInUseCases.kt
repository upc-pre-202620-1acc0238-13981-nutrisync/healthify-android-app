package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.repository.SelfWeighInRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import javax.inject.Inject

/** Saves the self weigh-in (or queues it when offline). */
class RecordSelfWeighInUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    suspend operator fun invoke(patientUserId: Long, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome> =
        repository.record(PatientId(patientUserId), weighIn)
}

/** The trend for the last [weeks] weeks (4 by default, like the backend). */
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

/** The self weigh-ins waiting in the phone's queue. */
class ObservePendingSelfWeighInsUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<List<PendingSelfWeighIn>> =
        repository.observePending(PatientId(patientUserId))
}

/** The backend accepted queued self weigh-ins: the trend has to be read again. */
class ObserveSyncedSelfWeighInsUseCase @Inject constructor(
    private val repository: SelfWeighInRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.syncedWeighIns
}

/** The Snackbar for the latest self weigh-in. */
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
