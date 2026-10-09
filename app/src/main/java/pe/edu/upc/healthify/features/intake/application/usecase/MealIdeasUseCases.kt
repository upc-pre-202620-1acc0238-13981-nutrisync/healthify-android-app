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

/** PT14.4 · Ideas para hoy (IA-3). */
class GetMealIdeasUseCase @Inject constructor(
    private val repository: MealIdeasRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        localDate: LocalDate,
        excludeIdeaIds: List<String> = emptyList(),
    ): Result<MealIdeas> = repository.generate(PatientId(patientUserId), localDate, excludeIdeaIds)
}

/** Resultado de registrar una idea: cómo terminó y qué ingredientes quedaron fuera por no estar en el catálogo. */
data class MealIdeaLogResult(val outcome: MealLogOutcome, val excluded: List<MealIdeaIngredient>)

/**
 * PT14.5 «Registrar esta comida» (IN-6): `manual-logs/batch` con los ingredientes **resueltos** (FC-2), `InPlan` y
 * `origin = MealIdea`. Los ingredientes con `resolved = false` se excluyen del registro y se devuelven para avisar.
 *
 * [clientEntryIds] se reusan en cada reintento (uno por ingrediente registrable, en orden): así reenviar la misma
 * comida nunca la duplica.
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
        /** Ningún ingrediente de la idea está en el catálogo: no hay nada que registrar. */
        const val CODE_NOTHING_TO_LOG = "NoResolvedIngredients"
    }
}
