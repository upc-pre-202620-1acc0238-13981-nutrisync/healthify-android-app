package pe.edu.upc.healthify.features.intake.domain.entity

import java.time.LocalDate

/**
 * What the patient logged on their day and what is left of each target (`DailyIntakeResource`), for the daily
 * gauge. Only confirmed entries count. It describes the day, never grades it.
 *
 * @param remainingEnergyKcal what is left of each target, never below 0 (the backend computes it).
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
