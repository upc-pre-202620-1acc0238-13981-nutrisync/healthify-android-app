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
 * Diario del paciente (§5.5, 100 % `Patient`). No hay borrado en ninguna parte (*Diary Entry Never Deleted*).
 *
 * Los registros (`log*`) intentan el backend y, **sin conexión**, encolan la comida en el teléfono con su
 * `clientEntryId` y su hora local (→ `POST /diary-entries/synchronization`); el resultado es entonces
 * [MealLogOutcome.QUEUED]. Cualquier otro fallo vuelve como error para que la pantalla lo muestre (PT7.2, PT10.3).
 */
interface DiaryRepository {

    /** `GET /patients/{pid}/diary-entries?date=` con la copia del teléfono si no hay conexión. */
    suspend fun getDiaryDay(patientId: PatientId, date: LocalDate): Result<DiaryDay>

    /** Comidas encoladas del paciente (pendientes y rechazadas), en el orden en que se registraron. */
    fun observePendingEntries(patientId: PatientId): Flow<List<PendingDiaryEntry>>

    /**
     * Emite cuando el backend aceptó comidas de la cola: ya están en el servidor y salieron de la cola, así que lo que
     * se leyó antes (el día, las kcal) hay que volver a leerlo.
     */
    val syncedEntries: Flow<Unit>

    suspend fun logManualMeal(patientId: PatientId, meal: NewManualMeal): Result<MealLogOutcome>

    suspend fun logPhotoMeal(patientId: PatientId, meal: NewPhotoMeal): Result<MealLogOutcome>

    /**
     * PT7 «Si sales sin confirmar, la comida queda en tu diario como «Por confirmar»»: `photo-logs` con el análisis y
     * sin confirmación. Sin conexión se encola como foto sin confirmar.
     */
    suspend fun keepPhotoMealUnconfirmed(
        patientId: PatientId,
        analysis: MealPhotoAnalysis,
        localTimestamp: LocalTimestamp,
        clientEntryId: ClientEntryId,
    ): Result<MealLogOutcome>

    suspend fun logMealGroup(patientId: PatientId, group: NewMealGroup): Result<MealLogOutcome>

    /** PT14 «Confirmar porción» sin cambios: `POST /diary-entries/{id}/estimate-confirmation`. */
    suspend fun confirmEstimate(entryId: DiaryEntryId, planAdherence: PlanAdherence): Result<Unit>

    /** PT8 con otro plato u otros gramos: `POST /diary-entries/{id}/estimate-adjustment`. */
    suspend fun adjustEstimate(
        entryId: DiaryEntryId,
        food: MealFood,
        portion: PortionGrams,
        planAdherence: PlanAdherence,
    ): Result<Unit>

    /** Pide enviar la cola ahora (PT19); el envío real lo hace WorkManager cuando hay red. */
    fun requestSync()

    /** El último registro hecho, para el Snackbar del diario (PT14.1); `null` si ya se mostró. */
    val loggedMealNotice: Flow<LoggedMealNotice?>

    fun consumeLoggedMealNotice()
}
