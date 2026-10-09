package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import java.time.Instant

/**
 * «Mis metas del día» (`ActiveTargetsResource`): la versión del plan vigente tal como le llega al paciente. Se guarda
 * en el dispositivo para verla sin conexión (*Cache Survives Offline*).
 *
 * @param validFrom desde cuándo rige (se muestra como «Publicado el …»).
 * @param restrictions códigos de la lista cerrada y, al final, las restricciones legadas (texto libre).
 * @param patientMessage mensaje del nutricionista para esta versión (NC-9); texto de una persona, nunca se traduce.
 */
data class ActiveTargets(
    val patientId: PatientId,
    val planVersion: Int,
    val validFrom: Instant,
    val targets: DailyTargets,
    val guidelines: List<PlanGuideline>,
    val restrictions: List<DietaryRestriction>,
    val changesFromPrevious: List<PlanChange>,
    val patientMessage: String?,
) {
    init {
        require(planVersion > 0) { "Plan version must be positive" }
        require(patientMessage == null || patientMessage.isNotBlank()) { "An empty message must be null" }
    }
}

/**
 * Resultado de pedir las metas vigentes.
 *
 * @param targets `null` si el paciente todavía no tiene metas publicadas (PT3.V).
 * @param savedAt cuándo se guardó la copia del dispositivo; solo si [fromCache] (PT3.O «actualizadas hoy 8:10»).
 */
data class ActiveTargetsLookup(
    val targets: ActiveTargets?,
    val fromCache: Boolean = false,
    val savedAt: Instant? = null,
) {
    init {
        require(!fromCache || (targets != null && savedAt != null)) { "A cached lookup needs targets and savedAt" }
    }

    companion object {
        val None = ActiveTargetsLookup(targets = null)
    }
}
