package pe.edu.upc.healthify.features.monitoring.domain.valueobject

import java.time.LocalDate

/**
 * Hueco de registro (F24, PT18 «Te extrañamos por acá»). El backend lo detecta en su reloj pero no lo expone por
 * REST (el recordatorio es local), así que el teléfono aplica la misma regla:
 * `today − último día registrado ≥ 3` (`Monitoring:LoggingGapThresholdDays`).
 *
 * DECISIÓN PT18: los días se leen de `daily-compliance?from=&to=` (los [THRESHOLD_DAYS] días hasta hoy, incluido);
 * solo hay hueco si el seguimiento ya llevaba ese tiempo (`window.From` no se expone: se usa desde cuándo rigen las
 * metas vigentes). Así un paciente recién vinculado no recibe un «hace unos días que no registras».
 *
 * Un hueco nunca es una desviación ni escala: solo invita a registrar (*Gap Is Not A Deviation*).
 */
object LoggingGap {

    const val THRESHOLD_DAYS = 3L

    /** Primer día del rango que se consulta: con hoy incluido, son [THRESHOLD_DAYS] días. */
    fun lookbackStart(today: LocalDate): LocalDate = today.minusDays(THRESHOLD_DAYS - 1)

    /**
     * @param loggedDaysInLookback días con al menos una entrada entre [lookbackStart] y [today].
     * @param trackingSince desde cuándo se le puede pedir registro al paciente.
     */
    fun isGap(loggedDaysInLookback: Int, trackingSince: LocalDate, today: LocalDate): Boolean {
        require(loggedDaysInLookback >= 0) { "Day counts cannot be negative" }
        val trackedLongEnough = !trackingSince.isAfter(today.minusDays(THRESHOLD_DAYS))
        return trackedLongEnough && loggedDaysInLookback == 0
    }
}
