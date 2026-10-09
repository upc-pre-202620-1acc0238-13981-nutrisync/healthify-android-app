package pe.edu.upc.healthify.features.intake.application

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.intake.application.usecase.ConfirmDiaryEntryUseCase
import pe.edu.upc.healthify.features.intake.application.usecase.LogMealIdeaUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.testing.FakeDiaryRepository
import java.time.OffsetDateTime

class IntakeUseCasesTest {

    private val diary = FakeDiaryRepository()
    private val ceviche = MealFood(12, "Ceviche")

    @Test
    fun `confirming the portion unchanged confirms the proposal`() = runTest {
        ConfirmDiaryEntryUseCase(diary)(DiaryEntryId(5), ceviche, 280.0, ceviche, PortionGrams(280.0), PlanAdherence.IN_PLAN)

        assertEquals(listOf(DiaryEntryId(5) to PlanAdherence.IN_PLAN), diary.confirmations)
        assertTrue(diary.adjustments.isEmpty())
    }

    @Test
    fun `changing the grams or the dish adjusts the proposal`() = runTest {
        val useCase = ConfirmDiaryEntryUseCase(diary)
        useCase(DiaryEntryId(5), ceviche, 280.0, ceviche, PortionGrams(250.0), PlanAdherence.OFF_PLAN)
        useCase(DiaryEntryId(6), ceviche, 280.0, MealFood(40, "Tiradito"), PortionGrams(280.0), PlanAdherence.IN_PLAN)

        assertEquals(listOf(250.0, 280.0), diary.adjustments.map { it.third.value })
        assertEquals(40L, diary.adjustments[1].second.referenceFoodId)
        assertTrue(diary.confirmations.isEmpty())
    }

    @Test
    fun `an idea logs only its resolved ingredients, in plan, with the given client entry ids`() = runTest {
        diary.logResult = Result.success(MealLogOutcome.QUEUED)
        val ids = listOf(ClientEntryId.random(), ClientEntryId.random())

        val result = LogMealIdeaUseCase(diary)(12, idea(), timestamp(), ids).getOrThrow()

        val group = diary.groups.single()
        assertEquals(listOf("Pechuga de pollo", "Camote"), group.items.map { it.food.name })
        assertEquals(ids, group.items.map { it.clientEntryId })
        assertEquals(PlanAdherence.IN_PLAN, group.planAdherence)
        assertEquals("idea-1", group.mealIdeaId)
        assertEquals(listOf("Aceite de oliva"), result.excluded.map { it.name })
        assertEquals(MealLogOutcome.QUEUED, result.outcome)
    }

    @Test
    fun `an idea with nothing in the catalog is not sent`() = runTest {
        val nothing = idea().copy(ingredients = listOf(MealIdeaIngredient("Aceite de oliva", 5.0, null)))

        val result = LogMealIdeaUseCase(diary)(12, nothing, timestamp(), emptyList())

        assertEquals(DomainError.Validation(LogMealIdeaUseCase.CODE_NOTHING_TO_LOG), result.domainErrorOrNull())
        assertTrue(diary.groups.isEmpty())
    }

    private fun timestamp() = LocalTimestamp.restore(OffsetDateTime.parse("2026-10-07T13:00:00-05:00"))

    private fun idea() = MealIdea(
        id = "idea-1",
        name = "Pollo al horno con camote",
        energyKcal = 540.0,
        proteinG = 38.0,
        carbG = 52.0,
        fatG = 14.0,
        ingredients = listOf(
            MealIdeaIngredient("Pechuga de pollo", 150.0, MealFood(231, "Pechuga de pollo")),
            MealIdeaIngredient("Camote", 120.0, MealFood(118, "Camote")),
            MealIdeaIngredient("Aceite de oliva", 5.0, null),
        ),
        why = "",
    )
}
