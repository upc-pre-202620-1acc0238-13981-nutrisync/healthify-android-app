package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.Instant
import java.time.LocalDate

/** Un punto de la serie suavizada: el promedio móvil de los autopesajes con protocolo hasta ese día. */
data class WeightTrendPoint(val date: LocalDate, val smoothedKg: Double) {
    init {
        require(smoothedKg.isFinite() && smoothedKg > 0) { "A smoothed value must be a positive weight" }
    }
}

/**
 * Tendencia de peso de PT13 (read model *Weight Trend Chart*, IN-5).
 *
 * Regla *Daily Figure Never Exposed As Headline*: esta entidad **no tiene** «último peso» ni «peso de hoy», igual que
 * el recurso del backend. Solo la serie suavizada y su resumen (pendiente y cambio), calculados sobre esa serie.
 *
 * @param points la serie completa, de la más antigua a la más reciente.
 * @param windowSize cuántos autopesajes se promedian en cada punto.
 * @param excludedReadingsCount autopesajes del rango sin protocolo: guardados, pero fuera de la línea.
 * @param changeKgOverRange último punto del rango menos el primero; `null` con menos de dos puntos.
 * @param fromCache viene de la copia del teléfono (sin conexión), guardada en [savedAt].
 */
data class WeightTrend(
    val points: List<WeightTrendPoint>,
    val windowSize: Int,
    val lastRecalculatedAt: Instant,
    val excludedReadingsCount: Int = 0,
    val changeKgOverRange: Double? = null,
    val slopeKgPerWeek: Double? = null,
    val rangeFrom: LocalDate? = null,
    val rangeTo: LocalDate? = null,
    val fromCache: Boolean = false,
    val savedAt: Instant? = null,
) {
    init {
        require(windowSize > 0) { "The moving average needs at least one reading" }
        require(excludedReadingsCount >= 0) { "Excluded readings cannot be negative" }
        require(points.zipWithNext().all { (a, b) -> a.date < b.date }) { "Points must be in date order, one per day" }
        require(rangeFrom == null || rangeTo == null || !rangeTo.isBefore(rangeFrom)) { "The range is inverted" }
        require(!fromCache || savedAt != null) { "A cached trend knows when it was saved" }
    }

    /** Los puntos del rango pedido (`?weeks=`); sin rango, toda la serie. */
    val rangePoints: List<WeightTrendPoint>
        get() = points.filter { point ->
            (rangeFrom == null || !point.date.isBefore(rangeFrom)) && (rangeTo == null || !point.date.isAfter(rangeTo))
        }

    /** PT13.V: hacen falta al menos 2 puntos en el rango para dibujar una línea. */
    val canBeDrawn: Boolean get() = rangePoints.size >= MIN_POINTS_TO_DRAW

    /** Dirección del cambio del rango, con una tolerancia de medio decimal (cero a la vista). */
    val direction: Direction?
        get() = changeKgOverRange?.let { change ->
            when {
                change >= STABLE_TOLERANCE_KG -> Direction.UP
                change <= -STABLE_TOLERANCE_KG -> Direction.DOWN
                else -> Direction.STABLE
            }
        }

    enum class Direction { UP, DOWN, STABLE }

    companion object {
        const val MIN_POINTS_TO_DRAW = 2

        /** Un cambio que se muestra como «0,0 kg» se dice «estable». */
        const val STABLE_TOLERANCE_KG = 0.05
    }
}
