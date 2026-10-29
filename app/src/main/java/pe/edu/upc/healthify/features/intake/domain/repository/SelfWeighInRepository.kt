package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId

/**
 * Self weigh-ins and weight trend.
 *
 * [record] tries the backend and, **when offline**, queues the self weigh-in with its `clientEntryId` and its local
 * time (→ `POST /self-weigh-ins/synchronization`); the result is then [SelfWeighInOutcome.QUEUED]. Any other
 * failure comes back as an error so the screen can show it.
 */
interface SelfWeighInRepository {

    suspend fun record(patientId: PatientId, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome>

    /** The patient's queued self weigh-ins (pending and rejected), in the order they were logged. */
    fun observePending(patientId: PatientId): Flow<List<PendingSelfWeighIn>>

    /** Emits when the backend accepted queued self weigh-ins: the trend read before has to be read again. */
    val syncedWeighIns: Flow<Unit>

    /**
     * `GET /patients/{pid}/weight-trend?weeks=`. `null` = there is no trend yet (`404 WeightTrendNotFound`).
     * Offline (or on 5xx) it returns the phone's latest copy, if any.
     */
    suspend fun getWeightTrend(patientId: PatientId, weeks: Int): Result<WeightTrend?>

    /** The latest saved self weigh-in, for the Snackbar; `null` if it was already shown. */
    val savedNotice: Flow<SelfWeighInOutcome?>

    fun consumeSavedNotice()
}
