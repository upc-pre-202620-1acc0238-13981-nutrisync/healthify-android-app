package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `PatientPlanVersionResource` (NC-8). Solo los campos que PT4.1 usa; las indicaciones, restricciones, cambios y el
 * mensaje de la versión vigente se leen de `active-targets` (que además queda guardado para verlo sin conexión).
 */
@Serializable
data class PlanVersionDto(
    val version: Int,
    val publishedAt: String,
    val isActive: Boolean = false,
    val energyKcal: Double,
)
