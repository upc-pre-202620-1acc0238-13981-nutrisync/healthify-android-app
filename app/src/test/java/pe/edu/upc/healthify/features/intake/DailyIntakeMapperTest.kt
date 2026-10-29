package pe.edu.upc.healthify.features.intake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DailyIntakeDto
import java.time.LocalDate

class DailyIntakeMapperTest {

    private fun dto(date: String = "2026-10-07", remainingFatG: Double = 19.0) = DailyIntakeDto(
        date = date,
        energyKcal = 1260.0,
        proteinG = 52.0,
        carbG = 150.0,
        fatG = 41.0,
        entryCount = 3,
        remainingEnergyKcal = 590.0,
        remainingProteinG = 38.0,
        remainingCarbG = 88.0,
        remainingFatG = remainingFatG,
    )

    @Test
    fun `the day's intake keeps what is left as the backend computed it`() {
        val intake = dto().toDomainOrNull()!!

        assertEquals(LocalDate.of(2026, 10, 7), intake.date)
        assertEquals(listOf(590.0, 38.0, 88.0, 19.0), listOf(intake.remainingEnergyKcal, intake.remainingProteinG, intake.remainingCarbG, intake.remainingFatG))
    }

    @Test
    fun `an unreadable resource is not shown`() {
        assertNull(dto(date = "ayer").toDomainOrNull())
        assertNull(dto(remainingFatG = -1.0).toDomainOrNull())
    }
}
