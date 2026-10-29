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
        fun fromCode(code: String?): ComplianceOutcome? = entries.firstOrNull { it.code == code }
    }
}
