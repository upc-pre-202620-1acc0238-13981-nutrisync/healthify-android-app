package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg

/**
 * Un autopesaje nuevo (PT12, F19). IN-3: el único protocolo que se pregunta es «¿Te pesaste en ayunas?»; una lectura
 * sin ayuno se guarda completa y solo no suaviza la tendencia (nada se rechaza ni se marca).
 *
 * [clientEntryId] se genera una vez por autopesaje: si queda en la cola offline, se reenvía siempre igual a
 * `…/synchronization` y el backend no lo duplica (IN-4).
 */
data class NewSelfWeighIn(
    val weight: WeightKg,
    val localTimestamp: LocalTimestamp,
    val fasted: Boolean,
    val clientEntryId: ClientEntryId,
)

/** Qué pasó al guardar: llegó al backend o quedó en el teléfono hasta tener conexión. */
enum class SelfWeighInOutcome { RECORDED, QUEUED }

/**
 * Un autopesaje que espera en la cola del teléfono (PT19). Se muestra la hora tal como se guardó.
 *
 * @param rejectionCode el backend no lo aceptó al sincronizar (`ImplausibleWeightValue`…): queda a la vista y no se
 *   reintenta.
 */
data class PendingSelfWeighIn(
    val clientEntryId: ClientEntryId,
    val weight: WeightKg,
    val localTimestamp: LocalTimestamp,
    val fasted: Boolean,
    val rejectionCode: String? = null,
) {
    val isRejected: Boolean get() = rejectionCode != null
}
