package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.entity.DailyIntake
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/** Metas vigentes del paciente con sesión (`GET /patients/{patientId}/active-targets`) y su copia offline. */
interface ActiveTargetsRepository {

    /**
     * Pide las metas al backend y actualiza la copia del dispositivo.
     * - `404 ActiveTargetsCacheNotFound` → sin metas ([ActiveTargetsLookup.None]) y se borra la copia.
     * - Cualquier otro fallo con copia guardada → la copia (`fromCache = true`).
     * - Cualquier otro fallo sin copia → el error.
     */
    suspend fun getActiveTargets(patientId: PatientId): Result<ActiveTargetsLookup>

    /**
     * IN-9 · lo registrado en [date] y lo que queda de cada meta (`GET /patients/{pid}/daily-intake`). Solo en línea:
     * no se guarda copia (sin conexión el medidor muestra las metas).
     */
    suspend fun getDailyIntake(patientId: PatientId, date: LocalDate): Result<DailyIntake>
}
