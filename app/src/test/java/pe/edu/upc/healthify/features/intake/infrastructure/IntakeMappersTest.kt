package pe.edu.upc.healthify.features.intake.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toPendingDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeaDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeaIngredientDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeasDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealPhotoAlternativeDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealPhotoAnalysisDto
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedDiaryEntryPayload

class IntakeMappersTest {

    @Test
    fun `a diary entry keeps proposal, confirmation, plan answer and meal group`() {
        val entry = DiaryEntryDto(
            diaryEntryId = 5,
            patientId = 12,
            localTimestamp = "2026-10-07T13:15:00-05:00",
            provenance = "Photo",
            proposedReferenceFoodId = 12,
            proposedPortionGrams = 320.0,
            confidence = 0.82,
            confirmedReferenceFoodId = 12,
            confirmedPortionGrams = 300.0,
            planAdherence = "OffPlan",
            isCountedTowardsTargets = true,
            foodName = " Lomo saltado ",
            mealGroupId = "b7a0c1d2-0000-4000-8000-000000000001",
            origin = "MealIdea",
        ).toDomainOrNull()!!

        assertEquals("Lomo saltado", entry.foodName)
        assertEquals(320.0, entry.proposedGrams!!, 0.0)
        assertEquals(300.0, entry.confirmedGrams!!, 0.0)
        assertEquals(PlanAdherence.OFF_PLAN, entry.planAdherence)
        assertTrue(entry.isOffPlan)
        assertTrue(entry.fromMealIdea)
        assertFalse(entry.isPendingConfirmation)
        assertEquals("2026-10-07T13:15-05:00", entry.localTimestamp.toString())
    }

    @Test
    fun `a historical off-plan entry without food is shown, an unreadable one is skipped`() {
        val legacy = DiaryEntryDto(diaryEntryId = 1, patientId = 12, localTimestamp = "2026-10-07T20:40:00-05:00", provenance = "OffPlan")
            .toDomainOrNull()!!
        assertEquals(EntryProvenance.OFF_PLAN, legacy.provenance)
        assertTrue(legacy.isOffPlan)
        assertNull(legacy.displayedGrams)

        assertNull(DiaryEntryDto(diaryEntryId = 2, patientId = 12, localTimestamp = "ayer", provenance = "Manual").toDomainOrNull())
        assertNull(DiaryEntryDto(diaryEntryId = 3, patientId = 12, localTimestamp = "2026-10-07T20:40:00Z", provenance = "Voice").toDomainOrNull())
        assertNull(
            DiaryEntryDto(diaryEntryId = 4, patientId = 12, localTimestamp = "2026-10-07T20:40:00Z", provenance = "Photo", confidence = 7.0)
                .toDomainOrNull(),
        )
    }

    @Test
    fun `a photo analysis resolves only catalog alternatives`() {
        val analysis = MealPhotoAnalysisDto(
            analysisId = "8f14e45f-ceea-467a-9a3b-2c0b1e5f9d10",
            referenceFoodId = 12,
            foodName = "Lomo saltado",
            estimatedGrams = 320.0,
            confidence = 0.82,
            alternatives = listOf(
                MealPhotoAlternativeDto("Tallarín saltado", 300.0, 31),
                MealPhotoAlternativeDto("Pollo saltado", 280.0, null),
                MealPhotoAlternativeDto(" ", 100.0, 5),
            ),
            expiresAt = "2026-10-08T13:00:00Z",
        ).toDomain()

        assertEquals(2, analysis.alternatives.size)
        assertEquals(listOf("Tallarín saltado"), analysis.selectableAlternatives.map { it.name })
        assertThrows(IllegalArgumentException::class.java) {
            MealPhotoAnalysisDto("8f14e45f-ceea-467a-9a3b-2c0b1e5f9d10", 12, "Lomo", 320.0, 1.4, emptyList(), "2026-10-08T13:00:00Z").toDomain()
        }
    }

    @Test
    fun `meal ideas keep unresolved ingredients without a catalog food`() {
        val ideas = MealIdeasDto(
            localDate = "2026-10-07",
            remainingKcal = 590.0,
            restrictions = listOf("ShellfishFree", "Unknown"),
            ideas = listOf(
                MealIdeaDto(
                    mealIdeaId = "a",
                    name = "Pollo al horno",
                    energyKcal = 540.0,
                    proteinG = 38.0,
                    carbG = 52.0,
                    fatG = 14.0,
                    ingredients = listOf(
                        MealIdeaIngredientDto("Pechuga de pollo", 150.0, 231, "Pechuga de pollo cruda"),
                        MealIdeaIngredientDto("Aceite de oliva", 5.0, 44, resolved = false),
                    ),
                    why = "Te faltan 48 g de proteína hoy.",
                ),
            ),
        ).toDomain()

        assertEquals(listOf(RestrictionCode.SHELLFISH_FREE), ideas.restrictions)
        val idea = ideas.ideas.single()
        assertEquals("Pechuga de pollo cruda", idea.loggableIngredients.single().food!!.name)
        assertEquals(listOf("Aceite de oliva"), idea.excludedIngredients.map { it.name })
    }

    @Test
    fun `the queued payload is sent exactly as stored`() {
        val payload = QueuedDiaryEntryPayload(
            clientEntryId = "3f2a9c1e-5b7d-4e2f-8a1c-9d0e1f2a3b4c",
            localTimestamp = "2026-10-07T07:45:00-05:00",
            provenance = "Photo",
            referenceFoodId = 12,
            portionGrams = 280.0,
            foodName = "Ceviche",
            confidence = 0.64,
            confirmed = false,
        )

        val dto = payload.toPendingDto()
        assertEquals(payload.clientEntryId, dto.clientEntryId)
        assertEquals("2026-10-07T07:45:00-05:00", dto.localTimestamp)
        assertEquals(false, dto.confirmed)
        assertNull(dto.planAdherence)
        val pending = payload.toDomainOrNull(rejectionCode = null)!!
        assertFalse(pending.confirmed)
        assertEquals(PlanAdherence.NOT_ANSWERED, pending.planAdherence)
    }
}
