package pe.edu.upc.healthify.features.monitoring.domain.valueobject

/**
 * Resultado de un día (F21, `DailyComplianceResource.outcome`). [UNLOGGED] está **al lado** de los otros tres, no
 * debajo: un día sin registro no es evidencia de nada, nunca es incumplimiento y nunca escala.
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
