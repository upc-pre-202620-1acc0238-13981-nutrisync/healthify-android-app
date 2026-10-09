package pe.edu.upc.healthify.features.intake.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.MealGroupItem
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PhotoConfirmation
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.testing.mealPhotoAnalysis
import java.time.Instant
import java.time.OffsetDateTime

class IntakeLoggingDomainTest {

    private val now: Instant = Instant.parse("2026-10-07T18:00:00Z")
    private val lima: OffsetDateTime = OffsetDateTime.parse("2026-10-07T13:00:00-05:00") // = now

    // ----- LocalTimestamp: ventana retroactiva de 48 h -----

    @Test
    fun `a meal up to exactly 48 h ago is valid`() {
        assertEquals(LocalTimestamp.Validity.VALID, LocalTimestamp.validate(lima.minusHours(48), now))
        assertEquals(LocalTimestamp.Validity.VALID, LocalTimestamp.validate(lima.minusHours(2), now))
        assertEquals(lima.minusHours(48), LocalTimestamp.forNewEntry(lima.minusHours(48), now).value)
    }

    @Test
    fun `a meal older than 48 h is too old and cannot be created`() {
        val tooOld = lima.minusHours(48).minusMinutes(1)

        assertEquals(LocalTimestamp.Validity.TOO_OLD, LocalTimestamp.validate(tooOld, now))
        assertThrows(IllegalArgumentException::class.java) { LocalTimestamp.forNewEntry(tooOld, now) }
    }

    @Test
    fun `a meal in the future is rejected beyond a short tolerance`() {
        assertEquals(LocalTimestamp.Validity.VALID, LocalTimestamp.validate(lima.plusMinutes(3), now))
        assertEquals(LocalTimestamp.Validity.IN_FUTURE, LocalTimestamp.validate(lima.plusMinutes(10), now))
    }

    @Test
    fun `the window is measured on the instant, whatever the offset`() {
        // 47 h antes, escrito en UTC: sigue dentro de la ventana.
        val utc = OffsetDateTime.parse("2026-10-05T19:00:00Z")
        assertEquals(LocalTimestamp.Validity.VALID, LocalTimestamp.validate(utc, now))
    }

    @Test
    fun `a queued timestamp is restored as declared, without the window and with its offset`() {
        val old = OffsetDateTime.parse("2026-09-01T08:30:00-05:00")
        val restored = LocalTimestamp.restore(old)

        assertEquals(old, restored.value)
        assertEquals("2026-09-01", restored.localDate.toString())
        assertNotEquals(LocalTimestamp.restore(OffsetDateTime.parse("2026-09-01T13:30:00Z")), restored)
    }

    // ----- PortionGrams -----

    @Test
    fun `portions parse with comma or dot`() {
        assertEquals(150.0, PortionGrams.parseOrNull("150")!!.value, 0.0)
        assertEquals(150.5, PortionGrams.parseOrNull(" 150,5 ")!!.value, 0.0)
        assertEquals(62.25, PortionGrams.parseOrNull("62.25")!!.value, 0.0)
    }

    @Test
    fun `portions outside 1 to 2000 g or not numbers are rejected`() {
        listOf("", "0", "0.5", "2000.5", "2001", "abc", "-5", "1e3", "12,345").forEach {
            assertNull("«$it» should be invalid", PortionGrams.parseOrNull(it))
        }
        assertThrows(IllegalArgumentException::class.java) { PortionGrams(0.0) }
        assertThrows(IllegalArgumentException::class.java) { PortionGrams(Double.NaN) }
        assertEquals(2000.0, PortionGrams.parseOrNull("2000")!!.value, 0.0)
        assertNull(PortionGrams.ofOrNull(null))
    }

    // ----- PlanAdherence, ClientEntryId -----

    @Test
    fun `plan adherence comes from the yes-no answer and unknown codes stay unanswered`() {
        assertEquals(PlanAdherence.IN_PLAN, PlanAdherence.fromAnswer(true))
        assertEquals(PlanAdherence.OFF_PLAN, PlanAdherence.fromAnswer(false))
        assertEquals(PlanAdherence.OFF_PLAN, PlanAdherence.fromCode("offplan"))
        assertEquals(PlanAdherence.NOT_ANSWERED, PlanAdherence.fromCode("Maybe"))
        assertFalse(PlanAdherence.NOT_ANSWERED.isAnswered)
    }

    @Test
    fun `client entry ids are UUIDs`() {
        val id = ClientEntryId.random()
        assertEquals(id, ClientEntryId(id.value))
        assertThrows(IllegalArgumentException::class.java) { ClientEntryId("id-1") }
    }

    // ----- Registros nuevos -----

    @Test
    fun `a manual meal needs the plan answer`() {
        assertThrows(IllegalArgumentException::class.java) {
            NewManualMeal(
                food = MealFood(1, "Quinua cocida"),
                portion = PortionGrams(150.0),
                localTimestamp = LocalTimestamp.forNewEntry(lima, now),
                planAdherence = PlanAdherence.NOT_ANSWERED,
                clientEntryId = ClientEntryId.random(),
            )
        }
    }

    @Test
    fun `a confirmed photo logs the proposal or the adjustment`() {
        val analysis = mealPhotoAnalysis()
        val timestamp = LocalTimestamp.forNewEntry(lima, now)
        val asProposed = NewPhotoMeal(analysis, PhotoConfirmation.AsProposed, timestamp, PlanAdherence.IN_PLAN, ClientEntryId.random())
        val adjusted = asProposed.copy(confirmation = PhotoConfirmation.Adjusted(MealFood(31, "Tallarín saltado"), PortionGrams(300.0)))

        assertEquals("Lomo saltado", asProposed.loggedFood.name)
        assertEquals(320.0, asProposed.loggedGrams, 0.0)
        assertEquals(31L, adjusted.loggedFood.referenceFoodId)
        assertEquals(300.0, adjusted.loggedGrams, 0.0)
        assertEquals(2, analysis.alternatives.size)
        assertEquals(1, analysis.selectableAlternatives.size)
    }

    @Test
    fun `a meal group has 1 to 10 foods, each with its own client entry id`() {
        val timestamp = LocalTimestamp.forNewEntry(lima, now)
        fun item(id: ClientEntryId = ClientEntryId.random()) = MealGroupItem(MealFood(1, "Camote"), PortionGrams(120.0), id)

        assertThrows(IllegalArgumentException::class.java) { NewMealGroup(emptyList(), timestamp, "idea", "Idea") }
        assertThrows(IllegalArgumentException::class.java) { NewMealGroup(List(11) { item() }, timestamp, "idea", "Idea") }
        val repeated = ClientEntryId.random()
        assertThrows(IllegalArgumentException::class.java) { NewMealGroup(listOf(item(repeated), item(repeated)), timestamp, "idea", "Idea") }
        assertEquals(PlanAdherence.IN_PLAN, NewMealGroup(listOf(item()), timestamp, "idea", "Idea").planAdherence)
    }

    @Test
    fun `unresolved idea ingredients are excluded from logging`() {
        val idea = MealIdea(
            id = "idea-1",
            name = "Pollo al horno",
            energyKcal = 540.0,
            proteinG = 38.0,
            carbG = 52.0,
            fatG = 14.0,
            ingredients = listOf(
                MealIdeaIngredient("Pechuga de pollo", 150.0, MealFood(231, "Pechuga de pollo")),
                MealIdeaIngredient("Aceite de oliva", 5.0, null),
                MealIdeaIngredient("Sal", 0.5, MealFood(9, "Sal")),
            ),
            why = "",
        )

        assertEquals(listOf("Pechuga de pollo"), idea.loggableIngredients.map { it.name })
        assertEquals(listOf("Aceite de oliva", "Sal"), idea.excludedIngredients.map { it.name })
        assertTrue(idea.canBeLogged)
    }

    // ----- Diario -----

    @Test
    fun `a photo entry without confirmation is pending and shows its proposal`() {
        val pending = entry(1, "14:10", EntryProvenance.PHOTO, proposed = 280.0, confirmed = null)
        val confirmed = entry(2, "13:15", EntryProvenance.PHOTO, proposed = 320.0, confirmed = 300.0)

        assertTrue(pending.isPendingConfirmation)
        assertEquals(280.0, pending.displayedGrams!!, 0.0)
        assertFalse(confirmed.isPendingConfirmation)
        assertEquals(300.0, confirmed.displayedGrams!!, 0.0)
        assertThrows(IllegalArgumentException::class.java) { pending.copy(confidence = 1.2) }
    }

    @Test
    fun `the diary shows entries to confirm first, then newest to oldest`() {
        val day = DiaryDay(
            date = lima.toLocalDate(),
            entries = listOf(
                entry(1, "07:30", EntryProvenance.MANUAL, confirmed = 250.0),
                entry(2, "20:40", EntryProvenance.MANUAL, confirmed = 300.0),
                entry(3, "14:10", EntryProvenance.PHOTO, proposed = 280.0, confirmed = null),
            ),
        )

        assertEquals(listOf(3L, 2L, 1L), day.orderedEntries.map { it.id.value })
    }

    private fun entry(id: Long, time: String, provenance: EntryProvenance, proposed: Double? = null, confirmed: Double?) = DiaryEntry(
        id = DiaryEntryId(id),
        localTimestamp = LocalTimestamp.restore(OffsetDateTime.parse("2026-10-07T$time:00-05:00")),
        provenance = provenance,
        foodName = "Comida $id",
        proposedFoodId = proposed?.let { 10L },
        proposedGrams = proposed,
        confidence = proposed?.let { 0.64 },
        confirmedFoodId = confirmed?.let { 10L },
        confirmedGrams = confirmed,
        planAdherence = if (confirmed != null) PlanAdherence.IN_PLAN else PlanAdherence.NOT_ANSWERED,
        isCountedTowardsTargets = confirmed != null,
    )
}
