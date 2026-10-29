package pe.edu.upc.healthify.features.monitoring.domain.entity

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.Instant
import java.time.LocalDate

/**
 * `PatientMonitoringPanelResource`: the practitioner's Follow-up tab. **Read-only**: the practitioner never writes
 * or corrects the diary. It does not carry the consistency index (Patient First).
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
 * "This week": one outcome per day. [ComplianceOutcome.UNLOGGED] is shown separately ("No log"), never as
 * "Below" or as non-compliance.
 */
data class PanelWeek(val days: List<PanelDay>, val metDays: Int, val totalDays: Int) {
    init {
        require(metDays in 0..totalDays) { "Met days cannot exceed the total" }
    }
}

data class PanelDay(val date: LocalDate, val outcome: ComplianceOutcome)

/** "Logging · 6 of 7 days". */
data class DaysRatio(val count: Int, val total: Int) {
    init {
        require(count in 0..total) { "Count cannot exceed the total" }
    }
}

/** "Weight trend · −0.3 kg/wk · Self-weighing · 4 weeks". Never a single day's weight. */
data class PanelWeightTrend(val weeks: Int, val slopeKgPerWeek: Double?)

/** Source of a diary entry. */
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

/** An entry of "Today's diary" (read-only). `foodName` is the catalog name; it may be missing. */
data class PanelDiaryEntry(
    val id: Long,
    val localTimestamp: Instant,
    val foodName: String?,
    val grams: Double?,
    val provenance: PanelEntryProvenance,
    val isAwaitingConfirmation: Boolean,
)

/** Checkboxes of the clinical measurement protocol. */
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

/** "Last clinical measurement · Sep 3 · fasting, no shoes · 74.2 kg" (clinical series, apart from self-weighing). */
data class PanelClinicalMeasurement(
    val takenAt: Instant,
    val weightKg: Double,
    val protocolChecks: List<PanelProtocolCheck>,
)

/**
 * "AI-generated summary · Review it before using it in a visit". Without the patient's AI consent or with the
 * feature off, the backend sends only the data: [text] is `null` and the card is hidden.
 */
data class MonitoringSummary(val text: String?) {
    val hasText: Boolean get() = !text.isNullOrBlank()
}
