package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import java.time.Instant

/**
 * "My targets for the day" (`ActiveTargetsResource`): the active plan version as the patient receives it. It is
 * stored on the device so it can be seen offline (*Cache Survives Offline*).
 *
 * @param validFrom when it takes effect (shown as "Published on …").
 * @param restrictions codes from the closed list and, at the end, the legacy free-text restrictions.
 * @param patientMessage the nutritionist's message for this version; written by a person, never translated.
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
 * Result of asking for the active targets.
 *
 * @param targets `null` if the patient has no published targets yet.
 * @param savedAt when the device copy was saved; only if [fromCache] ("updated today 8:10").
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
