package pe.edu.upc.healthify.features.intake.domain.valueobject

/**
 * One line of "What changed in this version" (`changesFromPrevious`). The backend sends the type and its data; the
 * sentence is built on the device, in the reader's language.
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
         * Reads a `PlanChangeResource`. An unknown or incomplete type is skipped (`null`): the rest of the list is
         * still valid and no sentence is made up.
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

        /** For restrictions, `custom` is a legacy restriction that was left out of the closed list. */
        private fun restriction(code: String?, custom: String?): DietaryRestriction? =
            DietaryRestriction.ofCode(code) ?: DietaryRestriction.legacy(custom)
    }
}
