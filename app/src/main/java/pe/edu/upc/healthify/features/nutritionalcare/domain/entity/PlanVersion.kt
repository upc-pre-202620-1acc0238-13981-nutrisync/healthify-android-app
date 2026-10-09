package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import java.time.Instant

/**
 * Una versión publicada del plan, vista por el paciente (`PatientPlanVersionResource`, NC-8). El recurso es propio del
 * paciente: nunca trae diagnóstico, base de cálculo ni motivo de ajuste del profesional.
 *
 * PT4.1 solo muestra el número, la fecha, la energía y cuál está vigente; el detalle de la vigente lo da PT4.
 */
data class PlanVersion(
    val version: Int,
    val publishedAt: Instant,
    val isActive: Boolean,
    val energyKcal: Double,
) {
    init {
        require(version > 0) { "Plan versions are positive" }
        require(energyKcal > 0) { "A published version has a positive energy target" }
    }
}

/** PT4.1: la más reciente primero (el backend ya las ordena así; se reordena por si acaso). */
fun List<PlanVersion>.newestFirst(): List<PlanVersion> = sortedByDescending { it.version }
