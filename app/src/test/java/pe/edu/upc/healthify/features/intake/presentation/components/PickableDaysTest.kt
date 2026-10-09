package pe.edu.upc.healthify.features.intake.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PickableDaysTest {

    private val today = LocalDate.parse("2026-10-08")

    @Test
    fun `an old account offers today, yesterday and the day before`() {
        assertEquals(
            listOf(today, today.minusDays(1), today.minusDays(2)),
            pickableDays(today, LocalDate.parse("2025-01-01")),
        )
    }

    @Test
    fun `without the account date the 48 h window is kept`() {
        assertEquals(3, pickableDays(today, null).size)
    }

    @Test
    fun `an account created yesterday offers only today and yesterday`() {
        assertEquals(listOf(today, today.minusDays(1)), pickableDays(today, today.minusDays(1)))
    }

    @Test
    fun `an account created today offers only today, even with a clock behind the server`() {
        assertEquals(listOf(today), pickableDays(today, today))
        assertEquals(listOf(today), pickableDays(today, today.plusDays(1)))
    }
}
