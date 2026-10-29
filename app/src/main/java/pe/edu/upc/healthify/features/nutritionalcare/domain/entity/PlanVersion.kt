package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import java.time.Instant

/**
 * A published plan version, as the patient sees it (`PatientPlanVersionResource`). The resource belongs to the
 * patient: it never includes the diagnosis, calculation basis or the practitioner's adjustment reason.
 *
 * The list shows the number, date, energy and which one is current; tapping a version shows its detail (targets,
 * guidelines, restrictions, changes and message), read-only.
 */
data class PlanVersion(
    val version: Int,
    val publishedAt: Instant,
    val isActive: Boolean,
    val energyKcal: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val guidelines: List<PlanGuideline> = emptyList(),
    val restrictions: List<DietaryRestriction> = emptyList(),
    val changesFromPrevious: List<PlanChange> = emptyList(),
    val patientMessage: String? = null,
) {
    init {
        require(version > 0) { "Plan versions are positive" }
        require(energyKcal > 0) { "A published version has a positive energy target" }
    }
}

/** Newest first (the backend already sorts them this way; they are re-sorted just in case). */
fun List<PlanVersion>.newestFirst(): List<PlanVersion> = sortedByDescending { it.version }
