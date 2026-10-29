package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import java.time.Instant

/**
 * Una versión publicada del plan, vista por el paciente (`PatientPlanVersionResource`, NC-8). El recurso es propio del
 * paciente: nunca trae diagnóstico, base de cálculo ni motivo de ajuste del profesional.
 *
 * PT4.1 lista el número, la fecha, la energía y cuál está vigente; al tocar una versión se ve su detalle (metas,
 * indicaciones, restricciones, cambios y mensaje), solo de consulta.
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

/** PT4.1: la más reciente primero (el backend ya las ordena así; se reordena por si acaso). */
fun List<PlanVersion>.newestFirst(): List<PlanVersion> = sortedByDescending { it.version }
