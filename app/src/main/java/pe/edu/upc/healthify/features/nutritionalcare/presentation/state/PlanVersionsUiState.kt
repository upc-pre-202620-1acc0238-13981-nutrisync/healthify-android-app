package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import java.time.Instant

/**
 * PT4.1 · Versiones anteriores (solo de consulta). Sin conexión no hay lista: `plan-versions` no se guarda en el
 * teléfono (la versión vigente sí, en PT4).
 */
data class PlanVersionsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val versions: List<PlanVersionItem> = emptyList(),
    val loadFailed: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && !loadFailed && !isOffline && versions.isEmpty()
}

data class PlanVersionItem(
    val version: Int,
    val publishedAt: Instant,
    val energyKcal: Double,
    val isActive: Boolean,
)
