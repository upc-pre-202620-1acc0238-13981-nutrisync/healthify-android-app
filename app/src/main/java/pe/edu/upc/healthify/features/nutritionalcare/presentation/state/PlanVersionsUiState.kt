package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.core.designsystem.component.UiText
import java.time.Instant

/**
 * PT4.1 · Versiones anteriores (solo de consulta). Sin conexión no hay lista: `plan-versions` no se guarda en el
 * teléfono (la versión vigente sí, en PT4).
 *
 * @param selectedVersion la versión cuyo detalle está abierto (hoja inferior); `null` sin detalle.
 */
data class PlanVersionsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val versions: List<PlanVersionItem> = emptyList(),
    val loadFailed: Boolean = false,
    val selectedVersion: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && !loadFailed && !isOffline && versions.isEmpty()

    val selected: PlanVersionItem? get() = selectedVersion?.let { v -> versions.firstOrNull { it.version == v } }
}

/**
 * Una versión publicada lista para pintar, en la lista y en su detalle (paciente PT4.1 y nutricionista PAC-4). Los
 * códigos van como [UiText] traducibles y lo escrito por el nutricionista como `UiText.Raw`.
 */
data class PlanVersionItem(
    val version: Int,
    val publishedAt: Instant,
    val energyKcal: Double,
    val isActive: Boolean,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0,
    val guidelines: List<UiText> = emptyList(),
    val restrictions: List<UiText> = emptyList(),
    val changes: List<PlanChangeLine> = emptyList(),
    val patientMessage: String? = null,
)
