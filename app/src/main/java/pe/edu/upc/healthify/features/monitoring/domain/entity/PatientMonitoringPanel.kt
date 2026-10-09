package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.Instant
import java.time.LocalDate

/**
 * `PatientMonitoringPanelResource` (RM-3): la pestaña Seguimiento del profesional (PAC-2). **Solo lectura**: el
 * profesional nunca escribe ni corrige el diario (regla ética 5). No trae el índice de consistencia (Patient First).
 */
data class PatientMonitoringPanel(
    val date: LocalDate,
    val week: PanelWeek?,
    val loggedDays: DaysRatio?,
    val weightTrend: PanelWeightTrend?,
    val diary: List<PanelDiaryEntry>,
    val lastClinicalMeasurement: PanelClinicalMeasurement?,
)

/**
 * «Esta semana»: un resultado por día. [ComplianceOutcome.UNLOGGED] se muestra aparte («Sin registro»), nunca como
 * «Por debajo» ni como incumplimiento (regla ética 3).
 */
data class PanelWeek(val days: List<PanelDay>, val metDays: Int, val totalDays: Int) {
    init {
        require(metDays in 0..totalDays) { "Met days cannot exceed the total" }
    }
}

data class PanelDay(val date: LocalDate, val outcome: ComplianceOutcome)

/** «Registro · 6 de 7 días». */
data class DaysRatio(val count: Int, val total: Int) {
    init {
        require(count in 0..total) { "Count cannot exceed the total" }
    }
}

/** «Tendencia de peso · −0,3 kg/sem · Autopesaje · 4 semanas». Nunca el peso de un día (regla ética 6). */
data class PanelWeightTrend(val weeks: Int, val slopeKgPerWeek: Double?)

/** Procedencia de una entrada del diario. */
enum class PanelEntryProvenance(val code: String) {
    PHOTO("Photo"),
    MANUAL("Manual"),
    OFF_PLAN("OffPlan"),
    ;

    companion object {
        fun fromCode(code: String?): PanelEntryProvenance? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Una entrada del «Diario de hoy» (solo lectura). `foodName` es el nombre del catálogo; puede faltar. */
data class PanelDiaryEntry(
    val id: Long,
    val localTimestamp: Instant,
    val foodName: String?,
    val grams: Double?,
    val provenance: PanelEntryProvenance,
    val isAwaitingConfirmation: Boolean,
)

/** Casillas del protocolo de la medición clínica (NC-3). */
enum class PanelProtocolCheck(val code: String) {
    FASTING("Fasting"),
    NO_SHOES("NoShoes"),
    LIGHT_CLOTHING("LightClothing"),
    EMPTY_BLADDER("EmptyBladder"),
    SAME_SCALE("SameScale"),
    ;

    companion object {
        fun fromCode(code: String?): PanelProtocolCheck? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** «Última medición clínica · 3 sept. · en ayunas, sin zapatos · 74.2 kg» (serie clínica, aparte del autopesaje). */
data class PanelClinicalMeasurement(
    val takenAt: Instant,
    val weightKg: Double,
    val protocolChecks: List<PanelProtocolCheck>,
)

/**
 * IA-5 · «Resumen generado con IA · Revísalo antes de usarlo en consulta». Sin consentimiento de IA del paciente o con
 * la función apagada el backend manda solo los datos: [text] es `null` y la tarjeta no se muestra.
 */
data class MonitoringSummary(val text: String?) {
    val hasText: Boolean get() = !text.isNullOrBlank()
}
