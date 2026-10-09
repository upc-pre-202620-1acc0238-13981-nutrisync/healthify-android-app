package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * Una línea de «Qué cambió en esta versión» (NC-8, `changesFromPrevious`). El backend manda el tipo y sus datos; la
 * frase se arma en el dispositivo, en el idioma del lector.
 */
sealed interface PlanChange {
    data class GuidelineAdded(val guideline: PlanGuideline) : PlanChange
    data class GuidelineRemoved(val guideline: PlanGuideline) : PlanChange
    data class RestrictionAdded(val restriction: DietaryRestriction) : PlanChange
    data class RestrictionRemoved(val restriction: DietaryRestriction) : PlanChange
    data class EnergyChanged(val fromKcal: Double, val toKcal: Double) : PlanChange
    data class MacroChanged(val macro: Macronutrient, val fromG: Double, val toG: Double) : PlanChange
    data object NoTargetChanges : PlanChange

    companion object {
        /**
         * Lee un `PlanChangeResource`. DECISIÓN PT4: un tipo desconocido o incompleto se omite (`null`): el resto de
         * la lista sigue siendo válida y no se inventa una frase.
         */
        fun of(type: String, code: String?, custom: String?, macro: String?, from: Double?, to: Double?): PlanChange? =
            when (type) {
                "GuidelineAdded" -> PlanGuideline.of(code, custom)?.let(::GuidelineAdded)
                "GuidelineRemoved" -> PlanGuideline.of(code, custom)?.let(::GuidelineRemoved)
                "RestrictionAdded" -> restriction(code, custom)?.let(::RestrictionAdded)
                "RestrictionRemoved" -> restriction(code, custom)?.let(::RestrictionRemoved)
                "EnergyChanged" -> if (from != null && to != null) EnergyChanged(from, to) else null
                "MacroChanged" -> {
                    val known = Macronutrient.fromCode(macro)
                    if (known != null && from != null && to != null) MacroChanged(known, from, to) else null
                }
                "NoTargetChanges" -> NoTargetChanges
                else -> null
            }

        /** En las restricciones, `custom` es una restricción legada que quedó fuera de la lista cerrada. */
        private fun restriction(code: String?, custom: String?): DietaryRestriction? =
            DietaryRestriction.ofCode(code) ?: DietaryRestriction.legacy(custom)
    }
}
