package pe.edu.upc.healthify.features.carerelationship.domain.valueobject

/**
 * PR1 · por qué un paciente de la cartera requiere algo del nutricionista. El orden de declaración es la prioridad: si
 * cumple varios, cuenta el primero, y la tarjeta «Requieren acción» ordena las filas con este mismo criterio.
 */
enum class RosterAttention {
    /** Hay una señal abierta en la bandeja de revisión (`hasOpenReviewItem`). */
    OPEN_REVIEW,

    /** Quedó una consulta guiada a medias (`hasConsultationInProgress`). */
    CONSULTATION_IN_PROGRESS,

    /** Todavía sin datos base (`!hasBaseline`). */
    MISSING_BASELINE,

    /** Con datos base pero sin plan publicado (`isNew`). */
    PLAN_NOT_PUBLISHED,
}
