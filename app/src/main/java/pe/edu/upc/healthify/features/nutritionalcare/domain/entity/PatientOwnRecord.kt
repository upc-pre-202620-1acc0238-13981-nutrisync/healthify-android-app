package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * My record (`PatientOwnRecordResource`): the record the patient sees about themselves. By backend design it
 * **never** includes the diagnosis, calculation basis, BMI or its category (ethics rule 1). Each section can
 * come back empty (a context that did not respond) without breaking the rest.
 */
data class PatientOwnRecord(
    val practitioner: RecordPractitioner?,
    val nextFollowUp: RecordNextFollowUp?,
    val myNumbers: MyNumbers,
    val plan: RecordPlan?,
    val referrals: List<RecordReferral>,
) {
    /** "Referrals": only the open ones ("No active referrals" if none). */
    val openReferrals: List<RecordReferral> get() = referrals.filter { it.isOpen }
}

/** "Your nutritionist": the name as is, since when they follow the patient and whether the link is active. */
data class RecordPractitioner(
    val fullName: String?,
    val linkedSince: Instant?,
    val isActive: Boolean,
)

/** "My consultations · Next: …". */
data class RecordNextFollowUp(
    val followUpId: Long,
    val scheduledFor: Instant,
) {
    init {
        require(followUpId > 0) { "A follow up id is positive" }
    }
}

/** "MY NUMBERS": any number can be missing (no plan, no evaluated days, no clinical measurement, no trend). */
data class MyNumbers(
    val energyTargetKcal: Int?,
    val planVersion: Int?,
    val compliance: RecordCompliance?,
    val clinicalWeight: RecordClinicalWeight?,
    val weightSlopeKgPerWeek: Double?,
) {
    val isEmpty: Boolean
        get() = energyTargetKcal == null && compliance == null && clinicalWeight == null && weightSlopeKgPerWeek == null

    init {
        require(energyTargetKcal == null || energyTargetKcal > 0) { "An energy target is positive" }
        require(planVersion == null || planVersion > 0) { "A plan version is positive" }
    }

    companion object {
        val Empty = MyNumbers(null, null, null, null, null)
    }
}

/**
 * "My adherence 5 of 7 days": days with the target met over the days in the range. A day without logs does not count
 * as missed (ethics rule 3): the backend already leaves it out of [met] and the UI never shows it as a failure.
 */
data class RecordCompliance(
    val from: LocalDate,
    val to: LocalDate,
    val met: Int,
    val total: Int,
) {
    init {
        require(!to.isBefore(from)) { "A compliance range is not inverted" }
        require(total >= 0 && met in 0..total) { "Met days are between 0 and the total" }
    }

    /** Days in the range ("Last 7 days"). */
    val rangeDays: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
}

/** "Clinical weight · Assessment of Mar 3": the latest measurement from a consultation (never a self-weigh-in). */
data class RecordClinicalWeight(
    val kg: Double,
    val takenAt: Instant,
) {
    init {
        require(kg > 0) { "A clinical weight is positive" }
    }
}

/**
 * "MY PLAN": plan codes as they arrive (the app translates them in the presentation layer). A guideline is a catalog
 * `code` or `custom` text written by the nutritionist.
 */
data class RecordPlan(
    val guidelines: List<RecordGuideline>,
    val restrictions: List<String>,
    val legacyRestrictions: List<String>,
) {
    val isEmpty: Boolean get() = guidelines.isEmpty() && restrictions.isEmpty() && legacyRestrictions.isEmpty()
}

data class RecordGuideline(val code: String?, val custom: String?) {
    init {
        require(!code.isNullOrBlank() || !custom.isNullOrBlank()) { "A guideline has a code or a text" }
    }
}

/** A referral to another specialty. The nutritionist wrote the specialty: it is shown as is. */
data class RecordReferral(
    val id: Long,
    val specialty: String,
    val issuedAt: Instant,
    val isOpen: Boolean,
    val reason: String = "",
    val closedAt: Instant? = null,
)
