package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

/** Id de un ítem de la bandeja de revisión (`reviewItemId`). */
@JvmInline
value class ReviewItemId(val value: Long) {
    init {
        require(value > 0) { "ReviewItemId must be positive" }
    }
}

/** Filtro de `GET /review-items?state=` (NC-11): la bandeja pide los abiertos. */
enum class ReviewItemState(val code: String) {
    OPEN("Open"),
    RESOLVED("Resolved"),
    ;

    companion object {
        fun fromCode(code: String?): ReviewItemState? = entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/**
 * Qué señal abrió el ítem (`signalType`). Solo una desviación sostenida puede traer un plan propuesto por IA (NC-10).
 * Un tipo que esta versión no conoce se muestra tal cual (X-2: desconocido → `Custom`).
 */
sealed interface SignalType {
    data object SustainedDeviation : SignalType
    data object ScheduledRecheck : SignalType
    data object ConsistencyEscalation : SignalType

    data class Custom(val text: String) : SignalType {
        init {
            require(text.isNotBlank()) { "A custom signal type needs text" }
        }
    }

    companion object {
        fun of(code: String): SignalType = when (val trimmed = code.trim()) {
            "SustainedDeviation" -> SustainedDeviation
            "ScheduledRecheck" -> ScheduledRecheck
            "ConsistencyEscalation" -> ConsistencyEscalation
            else -> Custom(trimmed)
        }
    }
}

/** Hacia dónde se desvió la energía registrada respecto de la meta (`direction`). */
enum class DeviationDirection(val code: String) {
    ABOVE("Above"),
    BELOW("Below"),
    ;

    companion object {
        fun fromCode(code: String?): DeviationDirection? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}
