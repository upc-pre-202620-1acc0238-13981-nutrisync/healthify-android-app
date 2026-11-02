package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/**
 * A day's outcome (`DailyComplianceResource.outcome`). [UNLOGGED] sits **beside** the other three, not below
 * them: a day without logs is evidence of nothing, never non-compliance and never escalates.
 */
enum class ComplianceOutcome(val code: String) {
    MET("Met"),
    EXCEEDED("Exceeded"),
    SHORT("Short"),
    UNLOGGED("Unlogged"),
    ;

    companion object {
        /**
         * Band around the energy target that still counts as [MET] (backend `DailyCompliance.ToleranceRatio`): below
         * it the day is [SHORT], above it [EXCEEDED].
         */
        const val ENERGY_TOLERANCE_RATIO = 0.10

        fun fromCode(code: String?): ComplianceOutcome? = entries.firstOrNull { it.code == code }

        /** The outcome the backend would give [consumedKcal] against [targetKcal] (used until it answers). */
        fun forEnergy(consumedKcal: Double, targetKcal: Double): ComplianceOutcome {
            val tolerance = targetKcal * ENERGY_TOLERANCE_RATIO
            return when {
                consumedKcal > targetKcal + tolerance -> EXCEEDED
                consumedKcal < targetKcal - tolerance -> SHORT
                else -> MET
            }
        }
    }
}
