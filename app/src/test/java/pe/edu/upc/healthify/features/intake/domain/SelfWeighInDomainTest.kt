package pe.edu.upc.healthify.features.intake.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrend
import pe.edu.upc.healthify.features.intake.domain.entity.WeightTrendPoint
import pe.edu.upc.healthify.features.intake.domain.valueobject.WeightKg
import pe.edu.upc.healthify.testing.TODAY
import pe.edu.upc.healthify.testing.weightTrend
import java.time.Instant

class SelfWeighInDomainTest {

    @Test
    fun `a weight accepts a comma or a dot and up to two decimals`() {
        assertEquals(68.4, (WeightKg.parse("68,4") as WeightKg.Input.Valid).weight.value, 0.0)
        assertEquals(68.4, (WeightKg.parse(" 68.4 ") as WeightKg.Input.Valid).weight.value, 0.0)
        assertEquals(68.45, (WeightKg.parse("68,45") as WeightKg.Input.Valid).weight.value, 0.0)
        assertEquals(68.0, (WeightKg.parse("68") as WeightKg.Input.Valid).weight.value, 0.0)
    }

    @Test
    fun `the plausible range is 20 to 400 kg, both included`() {
        assertTrue(WeightKg.parse("20") is WeightKg.Input.Valid)
        assertTrue(WeightKg.parse("400") is WeightKg.Input.Valid)
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("19,99"))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("400.01"))
        // PT12.E: un cero de más o la coma olvidada.
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("684"))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("0"))
    }

    @Test
    fun `something that is not a weight is reported as out of range and an empty field as empty`() {
        assertEquals(WeightKg.Input.Empty, WeightKg.parse("   "))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("abc"))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("68,456"))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("-68"))
        assertEquals(WeightKg.Input.OutOfRange, WeightKg.parse("68,4,1"))
    }

    @Test
    fun `a weight is rounded to two decimals like the backend`() {
        assertEquals(68.46, WeightKg.of(68.456).value, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a weight below the range cannot be built`() {
        WeightKg.of(19.9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a weight that is not finite cannot be built`() {
        WeightKg.of(Double.NaN)
    }

    @Test
    fun `a stored value outside the range is not restored`() {
        assertNull(WeightKg.ofOrNull(500.0))
        assertNull(WeightKg.ofOrNull(null))
        assertEquals(70.0, WeightKg.ofOrNull(70.0)?.value)
    }

    @Test
    fun `a trend needs two points in its range to be drawn`() {
        assertTrue(weightTrend(days = 2).canBeDrawn)
        assertFalse(weightTrend(days = 1).canBeDrawn)
        assertFalse(weightTrend(days = 0).canBeDrawn)
    }

    @Test
    fun `points older than the range are not drawn`() {
        val old = WeightTrendPoint(TODAY.minusWeeks(10), 70.0)
        val trend = weightTrend(days = 3).let { it.copy(points = listOf(old) + it.points) }

        assertEquals(3, trend.rangePoints.size)
        assertFalse(old in trend.rangePoints)
    }

    @Test
    fun `the direction of the change uses a tolerance so 0,0 kg reads as stable`() {
        assertEquals(WeightTrend.Direction.UP, weightTrend(change = 0.6).direction)
        assertEquals(WeightTrend.Direction.DOWN, weightTrend(change = -0.3).direction)
        assertEquals(WeightTrend.Direction.STABLE, weightTrend(change = 0.04).direction)
        assertNull(weightTrend(change = null).direction)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `points out of date order are rejected`() {
        WeightTrend(
            points = listOf(WeightTrendPoint(TODAY, 68.0), WeightTrendPoint(TODAY.minusDays(1), 68.1)),
            windowSize = 7,
            lastRecalculatedAt = Instant.EPOCH,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a cached trend must know when it was saved`() {
        WeightTrend(points = emptyList(), windowSize = 7, lastRecalculatedAt = Instant.EPOCH, fromCache = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `excluded readings cannot be negative`() {
        WeightTrend(points = emptyList(), windowSize = 7, lastRecalculatedAt = Instant.EPOCH, excludedReadingsCount = -1)
    }
}
