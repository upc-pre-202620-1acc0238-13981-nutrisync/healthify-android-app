package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.intake.domain.entity.MealGroupItem
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.repository.MealIdeasRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.LocalDate
import javax.inject.Inject

/** Ideas for today. */
class GetMealIdeasUseCase @Inject constructor(
    private val repository: MealIdeasRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        localDate: LocalDate,
        excludeIdeaIds: List<String> = emptyList(),
    ): Result<MealIdeas> = repository.generate(PatientId(patientUserId), localDate, excludeIdeaIds)
}

/** Result of logging an idea: how it ended and which ingredients were left out because the catalog lacks them. */
data class MealIdeaLogResult(val outcome: MealLogOutcome, val excluded: List<MealIdeaIngredient>)

/**
 * "Log this meal": `manual-logs/batch` with the **resolved** ingredients, `InPlan` and `origin = MealIdea`.
 * Ingredients with `resolved = false` are left out of the log and returned so the screen can warn about them.
 *
 * [clientEntryIds] are reused on every retry (one per loggable ingredient, in order), so resending the same meal
 * never duplicates it.
 */
class LogMealIdeaUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        idea: MealIdea,
        localTimestamp: LocalTimestamp,
        clientEntryIds: List<ClientEntryId>,
    ): Result<MealIdeaLogResult> {
        val loggable = idea.loggableIngredients.take(NewMealGroup.MAX_ITEMS)
        if (loggable.isEmpty()) return domainFailure(DomainError.Validation(CODE_NOTHING_TO_LOG))
        require(clientEntryIds.size >= loggable.size) { "One clientEntryId per loggable ingredient" }
        val items = loggable.mapIndexed { index, ingredient ->
            MealGroupItem(
                food = requireNotNull(ingredient.food),
                portion = PortionGrams(ingredient.grams),
                clientEntryId = clientEntryIds[index],
            )
        }
        val group = NewMealGroup(items = items, localTimestamp = localTimestamp, mealIdeaId = idea.id, description = idea.name)
        return repository.logMealGroup(PatientId(patientUserId), group)
            .map { MealIdeaLogResult(outcome = it, excluded = idea.excludedIngredients) }
    }

    companion object {
        /** None of the idea's ingredients is in the catalog: there is nothing to log. */
        const val CODE_NOTHING_TO_LOG = "NoResolvedIngredients"
    }
}
