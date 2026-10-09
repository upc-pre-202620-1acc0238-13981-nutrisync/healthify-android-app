package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.core.designsystem.component.UiText
import java.time.Instant

/**
 * PT4 · Mi plan.
 *
 * @param plan la versión vigente; `null` mientras carga, si no hay metas ([hasNoTargets]) o si falló sin copia.
 * @param cachedAt cuándo se guardó la copia que se está mostrando (sin conexión); `null` si es del backend.
 * @param loadFailed error sin copia guardada: estado de error con «Reintentar».
 */
data class MyPlanUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val plan: PlanDetails? = null,
    val cachedAt: Instant? = null,
    val hasNoTargets: Boolean = false,
    val loadFailed: Boolean = false,
    val isAcknowledging: Boolean = false,
    val showServerError: Boolean = false,
)

/**
 * La versión vigente lista para pintar: los códigos ya convertidos en [UiText] (traducibles) y los textos de una
 * persona como `UiText.Raw` (tal cual).
 *
 * @param proteinShare parte de la energía diaria que aporta cada macro (barras de «Metas de cada día»).
 */
data class PlanDetails(
    val planVersion: Int,
    val publishedAt: Instant,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val proteinShare: Float,
    val carbShare: Float,
    val fatShare: Float,
    val guidelines: List<UiText>,
    val restrictions: List<UiText>,
    val changes: List<PlanChangeLine>,
    val patientMessage: String?,
)

/** Una línea de «Qué cambió en esta versión»; [secondary] = texto de apoyo («Las metas … no cambiaron»). */
data class PlanChangeLine(val text: UiText, val secondary: Boolean)

sealed interface MyPlanEvent {
    /** «Ya las revisé» → PT3. */
    data object NavigateBack : MyPlanEvent
}
