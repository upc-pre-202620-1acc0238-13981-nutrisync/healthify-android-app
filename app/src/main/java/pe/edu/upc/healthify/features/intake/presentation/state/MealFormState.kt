package pe.edu.upc.healthify.features.intake.presentation.state

import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Los campos comunes de PT8 y PT10.1: porción, «¿Cuándo comiste?» y «¿Estaba en tu plan?». Los errores solo se
 * muestran después de intentar confirmar (o si el backend rechaza el momento).
 */
data class MealFormState(
    val portionText: String = "",
    val mealTime: OffsetDateTime,
    val inPlan: Boolean? = null,
    val showPortionError: Boolean = false,
    val mealTimeError: LocalTimestamp.Validity? = null,
    val showPlanError: Boolean = false,
    val showTimePicker: Boolean = false,
) {
    /**
     * Aplica las reglas de los value objects (porción 1–2000 g, ventana de 48 h, respuesta Sí/No) y deja visibles los
     * errores. Con [checkTime] en `false` el momento se respeta tal cual (una entrada ya registrada no se reescribe).
     */
    fun validate(now: Instant, checkTime: Boolean = true): MealFormValidation {
        val portion = PortionGrams.parseOrNull(portionText)
        val validity = if (checkTime) LocalTimestamp.validate(mealTime, now) else LocalTimestamp.Validity.VALID
        val timestamp = when {
            validity != LocalTimestamp.Validity.VALID -> null
            checkTime -> LocalTimestamp.forNewEntry(mealTime, now)
            else -> LocalTimestamp.restore(mealTime)
        }
        val adherence = inPlan?.let(PlanAdherence::fromAnswer)
        return MealFormValidation(
            form = copy(
                showPortionError = portion == null,
                mealTimeError = validity.takeIf { it != LocalTimestamp.Validity.VALID },
                showPlanError = adherence == null,
            ),
            portion = portion,
            timestamp = timestamp,
            planAdherence = adherence,
        )
    }

    companion object {
        fun now(clock: Clock, portionText: String = ""): MealFormState =
            MealFormState(portionText = portionText, mealTime = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))

        /** La fecha y hora que eligió el paciente con el offset que tenía el teléfono en ese momento. */
        fun offsetOf(dateTime: LocalDateTime, clock: Clock): OffsetDateTime = dateTime.atZone(clock.zone).toOffsetDateTime()

        fun today(clock: Clock): LocalDate = LocalDate.now(clock)
    }
}

/** Resultado de [MealFormState.validate]: los valores ya construidos (o `null` si su regla falló). */
data class MealFormValidation(
    val form: MealFormState,
    val portion: PortionGrams?,
    val timestamp: LocalTimestamp?,
    val planAdherence: PlanAdherence?,
) {
    val isValid: Boolean get() = portion != null && timestamp != null && planAdherence != null
}
