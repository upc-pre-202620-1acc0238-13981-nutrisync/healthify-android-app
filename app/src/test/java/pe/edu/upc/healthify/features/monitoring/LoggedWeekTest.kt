package pe.edu.upc.healthify.features.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.upc.healthify.features.monitoring.domain.entity.ComplianceDay
import pe.edu.upc.healthify.features.monitoring.domain.entity.LoggedWeek
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ComplianceOutcome
import pe.edu.upc.healthify.features.monitoring.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.dto.ComplianceDayDto
import java.time.LocalDate

class LoggedWeekTest {

    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun `a week has the 7 days up to today and a missing day is unlogged`() {
        val week = LoggedWeek.of(today, listOf(ComplianceDay(today.minusDays(2), ComplianceOutcome.MET)))

        assertEquals((6L downTo 0L).map { today.minusDays(it) }, week.days.map { it.date })
        assertEquals(1, week.loggedDays)
        assertEquals(ComplianceOutcome.UNLOGGED, week.days.first().outcome)
    }

    @Test
    fun `days outside the week are ignored`() {
        val week = LoggedWeek.of(today, listOf(ComplianceDay(today.minusDays(9), ComplianceOutcome.MET)))

        assertEquals(0, week.loggedDays)
    }

    @Test
    fun `a day with an unknown outcome is not read`() {
        assertNull(ComplianceDayDto("2026-10-07", "Great").toDomainOrNull())
        assertEquals(
            ComplianceDay(today, ComplianceOutcome.SHORT),
            ComplianceDayDto("2026-10-07", "Short").toDomainOrNull(),
        )
    }
}
