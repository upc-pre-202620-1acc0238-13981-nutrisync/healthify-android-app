package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.LocalDate

/**
 * Lo registrado por el paciente en su día y lo que le queda de cada meta (`DailyIntakeResource`, IN-9), para el
 * medidor de PT3. Solo cuentan las entradas confirmadas. Describe el día, nunca lo califica.
 *
 * @param remainingEnergyKcal lo que queda de cada meta, nunca menos de 0 (lo calcula el backend).
 */
data class DailyIntake(
    val date: LocalDate,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val entryCount: Int,
    val remainingEnergyKcal: Double,
    val remainingProteinG: Double,
    val remainingCarbG: Double,
    val remainingFatG: Double,
) {
    init {
        require(listOf(energyKcal, proteinG, carbG, fatG).all { it >= 0 }) { "Intake cannot be negative" }
        require(
            listOf(remainingEnergyKcal, remainingProteinG, remainingCarbG, remainingFatG).all { it >= 0 },
        ) { "What is left never goes below zero" }
    }
}
