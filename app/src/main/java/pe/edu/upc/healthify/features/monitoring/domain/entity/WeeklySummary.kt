package pe.edu.upc.healthify.features.monitoring.domain.entity

import java.time.Instant
import java.time.LocalDate

/**
 * Las cifras de la semana, calculadas sin IA (MA-6, IN-5). Un día sin registro **no** cuenta como meta incumplida
 * (`unloggedDays` es aparte de `metDays`).
 */
data class WeeklySummaryFacts(
    val metDays: Int,
    val totalDays: Int,
    val loggedDays: Int,
    val unloggedDays: Int,
    val weightChangeKg: Double?,
) {
    init {
        require(totalDays > 0) { "A week has days" }
        require(metDays in 0..totalDays && loggedDays in 0..totalDays && unloggedDays in 0..totalDays) {
            "Day counts must be within the week"
        }
        require(metDays <= loggedDays) { "A day within the targets is a logged day" }
    }
}

/**
 * Resumen semanal con IA (IA-2, PT13 «Tu semana», PT13.2). Lo escribe la IA cada lunes a partir del diario de la semana
 * anterior, ya en el idioma del lector; se muestra **tal cual** (no se traduce en la app).
 *
 * @param wentWell «LO QUE SALIÓ BIEN»: de 1 a 3 viñetas.
 * @param watchOut «EN QUÉ FIJARTE»: hasta 2, en tono de invitación.
 */
data class WeeklySummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val headline: String,
    val wentWell: List<String>,
    val watchOut: List<String>,
    val facts: WeeklySummaryFacts,
    val generatedAt: Instant,
) {
    init {
        require(headline.isNotBlank()) { "A weekly summary has a headline" }
        require(!weekEnd.isBefore(weekStart)) { "The week is inverted" }
        require(wentWell.none { it.isBlank() } && watchOut.none { it.isBlank() }) { "Bullets cannot be blank" }
    }
}

/** Qué hay para la card «Tu semana» y PT13.2. */
sealed interface WeeklySummaryAvailability {

    data class Ready(val summary: WeeklySummary) : WeeklySummaryAvailability

    /** PT13.2.V «Aún no hay resumen»: `404 NotEnoughData` (pocos días registrados o aún no generado). */
    data object NotYet : WeeklySummaryAvailability

    /**
     * La función está apagada para este paciente o sin consentimiento de IA (`403`), o la IA del servidor está apagada
     * (`503 AiFeatureDisabled`): la card no aparece.
     */
    data object Off : WeeklySummaryAvailability
}
