package pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject

/** Id of a review inbox item (`reviewItemId`). */
@JvmInline
value class ReviewItemId(val value: Long) {
    init {
        require(value > 0) { "ReviewItemId must be positive" }
    }
}

/** Filter for `GET /review-items?state=`: the inbox asks for the open ones. */
enum class ReviewItemState(val code: String) {
    OPEN("Open"),
    RESOLVED("Resolved"),
    ;

    companion object {
        fun fromCode(code: String?): ReviewItemState? = entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/**
 * Which signal opened the item (`signalType`). Only a sustained deviation can carry an AI-proposed plan.
 * A type this version does not know is shown as is (unknown → `Custom`).
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

/** Which way the logged energy deviated from the target (`direction`). */
enum class DeviationDirection(val code: String) {
    ABOVE("Above"),
    BELOW("Below"),
    ;

    companion object {
        fun fromCode(code: String?): DeviationDirection? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}
