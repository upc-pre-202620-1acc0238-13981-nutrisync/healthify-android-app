package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.entity.DailyIntake
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/** Active targets of the signed-in patient (`GET /patients/{patientId}/active-targets`) and their offline copy. */
interface ActiveTargetsRepository {

    /**
     * Asks the backend for the targets and updates the device copy.
     * - `404 ActiveTargetsCacheNotFound` → no targets ([ActiveTargetsLookup.None]) and the copy is deleted.
     * - Any other failure with a saved copy → the copy (`fromCache = true`).
     * - Any other failure without a copy → the error.
     */
    suspend fun getActiveTargets(patientId: PatientId): Result<ActiveTargetsLookup>

    /**
     * What was logged on [date] and what is left of each target (`GET /patients/{pid}/daily-intake`). Online only:
     * no copy is kept (offline the gauge shows the targets).
     */
    suspend fun getDailyIntake(patientId: PatientId, date: LocalDate): Result<DailyIntake>
}
