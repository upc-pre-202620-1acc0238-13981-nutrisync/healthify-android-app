package pe.edu.upc.healthify.features.main.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import java.time.Instant
import java.time.LocalDate

/**
 * PT3 · Inicio (+ PT3.L, PT3.V, PT3.O, aviso «Algo no cuadra», PT18, aviso de meta de energía superada y PT3.M).
 *
 * @param targets metas vigentes; `null` mientras carga o sin metas ([hasNoTargets], PT3.V).
 * @param cachedAt cuándo se guardó la copia mostrada (PT3.O); `null` si las metas son del backend.
 * @param consumedKcal energía registrada hoy (confirmada); `null` si todavía no se pudo leer (sin conexión).
 * @param todayOutcome resultado de hoy para la fila «Cómo voy hoy»; `null` = fila oculta.
 * @param macrosLeft lo que queda hoy de cada macro (IN-9, lo calcula el backend); `null` sin lectura: el medidor
 *   muestra las metas en gramos con «Tu meta de hoy».
 * @param week «Tu semana»: qué días de los últimos 7 tienen registro; `null` sin lectura (la tarjeta se oculta).
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
    val macrosLeft: HomeMacros? = null,
    val week: HomeWeek? = null,
    val todayOutcome: ComplianceOutcome? = null,
    val nextFollowUp: HomeFollowUp? = null,
    val showConsistencyCard: Boolean = false,
    val showLoggingGapCard: Boolean = false,
    val targetsChangedVersion: Int? = null,
) {
    /** PT16: sin conexión la tarjeta simplemente no aparece. */
    val consistencyCardVisible: Boolean get() = showConsistencyCard && !isOffline && targets != null

    val loggingGapCardVisible: Boolean get() = showLoggingGapCard && targets != null

    /**
     * Aviso «Mañana es otro día»: el día ya salió EXCEEDED o lo registrado supera la meta de energía.
     * Sin conexión o sin metas no aparece (el avance del día solo se lee en línea, desde Monitoring).
     */
    val energyExceededCardVisible: Boolean
        get() {
            val target = targets?.energyKcal ?: return false
            if (isOffline) return false
            val consumed = consumedKcal
            return todayOutcome == ComplianceOutcome.EXCEEDED || (consumed != null && consumed > target)
        }

    /** PT3.O: sin conexión no se muestran «Cómo voy hoy» ni «Próxima consulta» (viven en Monitoring). */
    val howAmIDoingVisible: Boolean get() = !isOffline && todayOutcome != null
    val nextFollowUpVisible: Boolean get() = !isOffline && nextFollowUp != null

    val targetsChangedSheetVisible: Boolean get() = targetsChangedVersion != null && !isOffline && targets != null

    /** PT3.O: sin conexión «Tu semana» no se muestra (vive en Monitoring). */
    val weekVisible: Boolean get() = !isOffline && week != null

    private val cachedNoticeVisible: Boolean get() = isOffline || cachedAt != null

    /**
     * DECISIÓN PT3 (sin scroll): Inicio muestra un solo aviso a la vez, el de mayor prioridad. «Algo no cuadra» primero
     * (pide revisar), luego la copia sin conexión (explica por qué faltan cifras), «Te extrañamos» y «Mañana es otro
     * día». Los demás vuelven a aparecer cuando el de arriba se va.
     */
    val activeNotice: HomeNotice?
        get() = when {
            consistencyCardVisible -> HomeNotice.CONSISTENCY
            cachedNoticeVisible -> HomeNotice.CACHED_TARGETS
            loggingGapCardVisible -> HomeNotice.LOGGING_GAP
            energyExceededCardVisible -> HomeNotice.ENERGY_EXCEEDED
            else -> null
        }

    /** Kcal que quedan hoy (nunca menos de 0); `null` sin lectura del día. */
    val energyLeftKcal: Double?
        get() {
            val target = targets?.energyKcal ?: return null
            return consumedKcal?.let { (target - it).coerceAtLeast(0.0) }
        }

    /** «Meta de hoy cumplida»: lo registrado ya alcanzó la meta de energía (arco lleno, sin rojo ni reproche). */
    val energyTargetReached: Boolean
        get() {
            val target = targets?.energyKcal ?: return false
            return consumedKcal?.let { it >= target } == true
        }
}

/** Los avisos de Inicio, de mayor a menor prioridad ([PatientHomeUiState.activeNotice]). */
enum class HomeNotice { CONSISTENCY, CACHED_TARGETS, LOGGING_GAP, ENERGY_EXCEEDED }

/** Gramos de cada macro (lo que queda hoy). */
data class HomeMacros(val proteinG: Double, val carbG: Double, val fatG: Double)

/** «Tu semana»: los 7 días hasta hoy, del más antiguo al de hoy. */
data class HomeWeek(val days: List<HomeDay>) {
    val loggedDays: Int get() = days.count { it.isLogged }
}

data class HomeDay(val date: LocalDate, val isLogged: Boolean, val isToday: Boolean)

/** Metas del día que muestra Inicio (el detalle completo está en PT4). */
data class HomeTargets(
    val planVersion: Int,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)

data class HomeFollowUp(val id: Long, val scheduledFor: Instant)
