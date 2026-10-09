package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.NewSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.PendingSelfWeighIn
import pe.edu.upc.healthify.features.intake.domain.entity.SelfWeighInOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId

/**
 * Autopesajes y tendencia de peso (F19, IN-3, IN-4, IN-5).
 *
 * [record] intenta el backend y, **sin conexión**, encola el autopesaje con su `clientEntryId` y su hora local
 * (→ `POST /self-weigh-ins/synchronization`); el resultado es entonces [SelfWeighInOutcome.QUEUED]. Cualquier otro
 * fallo vuelve como error para que PT12 lo muestre.
 */
interface SelfWeighInRepository {

    suspend fun record(patientId: PatientId, weighIn: NewSelfWeighIn): Result<SelfWeighInOutcome>

    /** Autopesajes encolados del paciente (pendientes y rechazados), en el orden en que se registraron. */
    fun observePending(patientId: PatientId): Flow<List<PendingSelfWeighIn>>

    /** Emite cuando el backend aceptó autopesajes de la cola: la tendencia leída antes hay que volver a leerla. */
    val syncedWeighIns: Flow<Unit>

    /**
     * `GET /patients/{pid}/weight-trend?weeks=`. `null` = todavía no hay tendencia (`404 WeightTrendNotFound`).
     * Sin conexión (o 5xx) devuelve la última copia del teléfono, si hay.
     */
    suspend fun getWeightTrend(patientId: PatientId, weeks: Int): Result<WeightTrend?>

    /** El último autopesaje guardado, para el Snackbar de PT13.1; `null` si ya se mostró. */
    val savedNotice: Flow<SelfWeighInOutcome?>

    fun consumeSavedNotice()
}
