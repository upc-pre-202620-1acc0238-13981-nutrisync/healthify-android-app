package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.Instant

/**
 * PT3 · Inicio (+ PT3.L, PT3.V, PT3.O, aviso «Algo no cuadra», PT18 y PT3.M).
 *
 * @param targets metas vigentes; `null` mientras carga o sin metas ([hasNoTargets], PT3.V).
 * @param cachedAt cuándo se guardó la copia mostrada (PT3.O); `null` si las metas son del backend.
 * @param consumedKcal energía registrada hoy (confirmada); `null` si todavía no se pudo leer (sin conexión).
 * @param todayOutcome resultado de hoy para la fila «Cómo voy hoy»; `null` = fila oculta.
 * @param nextFollowUp «Próxima consulta»; `null` = fila oculta (no hay ninguna agendada).
 * @param showConsistencyCard aviso «Algo no cuadra» (alerta de consistencia activa o aviso sin ver, MA-7).
 * @param showLoggingGapCard PT18 «Te extrañamos por acá».
 * @param targetsChangedVersion PT3.M: versión publicada que el paciente aún no revisó.
 */
data class PatientHomeUiState(
    val greetingName: String = "",
    val avatarInitial: String = "",
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val targets: HomeTargets? = null,
    val cachedAt: Instant? = null,
    val hasNoTargets: Boolean = false,
    val consumedKcal: Double? = null,
    val todayOutcome: ComplianceOutcome? = null,
    val nextFollowUp: HomeFollowUp? = null,
    val showConsistencyCard: Boolean = false,
    val showLoggingGapCard: Boolean = false,
    val targetsChangedVersion: Int? = null,
) {
    /** PT16: sin conexión la tarjeta simplemente no aparece. */
    val consistencyCardVisible: Boolean get() = showConsistencyCard && !isOffline && targets != null

    val loggingGapCardVisible: Boolean get() = showLoggingGapCard && targets != null

    /** PT3.O: sin conexión no se muestran «Cómo voy hoy» ni «Próxima consulta» (viven en Monitoring). */
    val howAmIDoingVisible: Boolean get() = !isOffline && todayOutcome != null
    val nextFollowUpVisible: Boolean get() = !isOffline && nextFollowUp != null

    val targetsChangedSheetVisible: Boolean get() = targetsChangedVersion != null && !isOffline && targets != null

    /** Con un aviso arriba, «Registrar comida» pasa a la variante horizontal de 56 dp (Notas de PT3 aviso / PT18). */
    val compactRegisterButton: Boolean get() = consistencyCardVisible || loggingGapCardVisible
}

/** Metas del día que muestra Inicio (el detalle completo está en PT4). */
data class HomeTargets(
    val planVersion: Int,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)

data class HomeFollowUp(val id: Long, val scheduledFor: Instant)
