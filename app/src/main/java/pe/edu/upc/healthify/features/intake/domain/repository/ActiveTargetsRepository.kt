package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId

/** Metas vigentes del paciente con sesión (`GET /patients/{patientId}/active-targets`) y su copia offline. */
interface ActiveTargetsRepository {

    /**
     * Pide las metas al backend y actualiza la copia del dispositivo.
     * - `404 ActiveTargetsCacheNotFound` → sin metas ([ActiveTargetsLookup.None]) y se borra la copia.
     * - Cualquier otro fallo con copia guardada → la copia (`fromCache = true`).
     * - Cualquier otro fallo sin copia → el error.
     */
    suspend fun getActiveTargets(patientId: PatientId): Result<ActiveTargetsLookup>
}
