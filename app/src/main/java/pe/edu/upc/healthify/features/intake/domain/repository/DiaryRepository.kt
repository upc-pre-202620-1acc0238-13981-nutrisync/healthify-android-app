package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.LocalDate

/**
 * The patient's diary (100 % `Patient`). There is no deletion anywhere (*Diary Entry Never Deleted*).
 *
 * The logs (`log*`) try the backend and, **when offline**, queue the meal on the phone with its
 * `clientEntryId` and its local time (→ `POST /diary-entries/synchronization`); the result is then
 * [MealLogOutcome.QUEUED]. Any other failure comes back as an error so the screen can show it.
 */
interface DiaryRepository {

    /** `GET /patients/{pid}/diary-entries?date=` with the phone's copy when offline. */
    suspend fun getDiaryDay(patientId: PatientId, date: LocalDate): Result<DiaryDay>

    /** The patient's queued meals (pending and rejected), in the order they were logged. */
    fun observePendingEntries(patientId: PatientId): Flow<List<PendingDiaryEntry>>

    /**
     * Emits when the backend accepted queued meals: they are on the server and out of the queue, so whatever was
     * read before (the day, the kcal) has to be read again.
     */
    val syncedEntries: Flow<Unit>

    suspend fun logManualMeal(patientId: PatientId, meal: NewManualMeal): Result<MealLogOutcome>

    suspend fun logPhotoMeal(patientId: PatientId, meal: NewPhotoMeal): Result<MealLogOutcome>

    /**
     * "If you leave without confirming, the meal stays in your diary as To confirm": `photo-logs` with the analysis
     * and no confirmation. When offline it is queued as an unconfirmed photo.
     */
    suspend fun keepPhotoMealUnconfirmed(
        patientId: PatientId,
        analysis: MealPhotoAnalysis,
        localTimestamp: LocalTimestamp,
        clientEntryId: ClientEntryId,
    ): Result<MealLogOutcome>

    suspend fun logMealGroup(patientId: PatientId, group: NewMealGroup): Result<MealLogOutcome>

    /** "Confirm portion" with no changes: `POST /diary-entries/{id}/estimate-confirmation`. */
    suspend fun confirmEstimate(entryId: DiaryEntryId, planAdherence: PlanAdherence): Result<Unit>

    /** Another dish or other grams: `POST /diary-entries/{id}/estimate-adjustment`. */
    suspend fun adjustEstimate(
        entryId: DiaryEntryId,
        food: MealFood,
        portion: PortionGrams,
        planAdherence: PlanAdherence,
    ): Result<Unit>

    /** Asks to send the queue now; WorkManager does the actual sending when there is network. */
    fun requestSync()

    /** The latest log, for the diary Snackbar; `null` if it was already shown. */
    val loggedMealNotice: Flow<LoggedMealNotice?>

    fun consumeLoggedMealNotice()
}
