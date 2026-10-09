package pe.edu.upc.healthify.features.intake.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.core.network.toOffsetDateTimeOrNull
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.MealGroupItem
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdea
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeaIngredient
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAlternative
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewMealGroup
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.entity.PhotoConfirmation
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.EntryProvenance
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealPhotoAnalysisId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ManualLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupItemDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealGroupOriginDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealIdeasDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.MealPhotoAnalysisDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PendingDiaryEntryDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PhotoConfirmationDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.PhotoLogRequestDto
import pe.edu.upc.healthify.features.intake.infrastructure.sync.QueuedDiaryEntryPayload
import java.time.format.DateTimeFormatter

/** ISO-8601 con offset (`2026-09-09T13:15:00-05:00`): el formato con que viaja y se guarda el momento declarado. */
fun LocalTimestamp.toIsoString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)

// ----- Diario -----

/** `null` si el recurso no se puede mostrar (momento ilegible, procedencia desconocida, cifras inválidas). */
fun DiaryEntryDto.toDomainOrNull(): DiaryEntry? {
    val timestamp = localTimestamp.toOffsetDateTimeOrNull() ?: return null
    val provenanceValue = EntryProvenance.fromCode(provenance) ?: return null
    return try {
        DiaryEntry(
            id = DiaryEntryId(diaryEntryId),
            localTimestamp = LocalTimestamp.restore(timestamp),
            provenance = provenanceValue,
            foodName = foodName?.trim()?.takeIf { it.isNotEmpty() },
            proposedFoodId = proposedReferenceFoodId?.takeIf { it > 0 },
            proposedGrams = proposedPortionGrams?.takeIf { it > 0 },
            confidence = confidence,
            confirmedFoodId = confirmedReferenceFoodId?.takeIf { it > 0 },
            confirmedGrams = confirmedPortionGrams?.takeIf { it > 0 },
            planAdherence = PlanAdherence.fromCode(planAdherence),
            isCountedTowardsTargets = isCountedTowardsTargets,
            mealGroupId = mealGroupId?.takeIf { it.isNotBlank() },
            fromMealIdea = origin.equals(ORIGIN_MEAL_IDEA, ignoreCase = true),
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun NewManualMeal.toRequestDto(patientId: Long) = ManualLogRequestDto(
    patientId = patientId,
    localTimestamp = localTimestamp.toIsoString(),
    referenceFoodId = food.referenceFoodId,
    portionGrams = portion.value,
    planAdherence = planAdherence.code,
    clientEntryId = clientEntryId.value,
)

fun NewManualMeal.toQueuedPayload() = QueuedDiaryEntryPayload(
    clientEntryId = clientEntryId.value,
    localTimestamp = localTimestamp.toIsoString(),
    provenance = EntryProvenance.MANUAL.code,
    referenceFoodId = food.referenceFoodId,
    portionGrams = portion.value,
    foodName = food.name,
    confirmed = true,
    planAdherence = planAdherence.code,
)

fun NewPhotoMeal.toRequestDto(patientId: Long) = PhotoLogRequestDto(
    patientId = patientId,
    localTimestamp = localTimestamp.toIsoString(),
    referenceFoodId = analysis.food.referenceFoodId,
    portionGrams = analysis.estimatedGrams,
    confidence = analysis.confidence,
    analysisId = analysis.id.value,
    clientEntryId = clientEntryId.value,
    confirmation = when (val c = confirmation) {
        PhotoConfirmation.AsProposed -> PhotoConfirmationDto(kind = CONFIRMATION_AS_PROPOSED)
        is PhotoConfirmation.Adjusted -> PhotoConfirmationDto(
            kind = CONFIRMATION_ADJUSTED,
            referenceFoodId = c.food.referenceFoodId,
            portionGrams = c.portion.value,
        )
    },
    planAdherence = planAdherence.code,
)

/**
 * Sin conexión, la foto confirmada se encola como `Photo` confirmada con el plato y los gramos que eligió el paciente
 * (la sincronización no lleva `analysisId`; la confianza viaja como dato de la estimación).
 */
fun NewPhotoMeal.toQueuedPayload() = QueuedDiaryEntryPayload(
    clientEntryId = clientEntryId.value,
    localTimestamp = localTimestamp.toIsoString(),
    provenance = EntryProvenance.PHOTO.code,
    referenceFoodId = loggedFood.referenceFoodId,
    portionGrams = loggedGrams,
    foodName = loggedFood.name,
    confidence = analysis.confidence,
    confirmed = true,
    planAdherence = planAdherence.code,
)

/** PT7 sin confirmar: «Por confirmar» (`photo-logs` sin `confirmation`). */
fun MealPhotoAnalysis.toUnconfirmedRequestDto(patientId: Long, localTimestamp: LocalTimestamp, clientEntryId: ClientEntryId) =
    PhotoLogRequestDto(
        patientId = patientId,
        localTimestamp = localTimestamp.toIsoString(),
        referenceFoodId = food.referenceFoodId,
        portionGrams = estimatedGrams,
        confidence = confidence,
        analysisId = id.value,
        clientEntryId = clientEntryId.value,
    )

fun MealPhotoAnalysis.toUnconfirmedQueuedPayload(localTimestamp: LocalTimestamp, clientEntryId: ClientEntryId) =
    QueuedDiaryEntryPayload(
        clientEntryId = clientEntryId.value,
        localTimestamp = localTimestamp.toIsoString(),
        provenance = EntryProvenance.PHOTO.code,
        referenceFoodId = food.referenceFoodId,
        portionGrams = estimatedGrams,
        foodName = food.name,
        confidence = confidence,
        confirmed = false,
        planAdherence = null,
    )

fun NewMealGroup.toRequestDto(patientId: Long) = MealGroupLogRequestDto(
    patientId = patientId,
    localTimestamp = localTimestamp.toIsoString(),
    items = items.map {
        MealGroupItemDto(referenceFoodId = it.food.referenceFoodId, portionGrams = it.portion.value, clientEntryId = it.clientEntryId.value)
    },
    planAdherence = planAdherence.code,
    origin = mealIdeaId?.let { MealGroupOriginDto(kind = ORIGIN_MEAL_IDEA, mealIdeaId = it) },
)

/**
 * DECISIÓN PT14.5: la sincronización no tiene lotes agrupados; sin conexión cada alimento de la idea se encola como
 * una comida a mano con el mismo momento (se pierde solo la agrupación visual).
 */
fun MealGroupItem.toQueuedPayload(group: NewMealGroup) = QueuedDiaryEntryPayload(
    clientEntryId = clientEntryId.value,
    localTimestamp = group.localTimestamp.toIsoString(),
    provenance = EntryProvenance.MANUAL.code,
    referenceFoodId = food.referenceFoodId,
    portionGrams = portion.value,
    foodName = food.name,
    confirmed = true,
    planAdherence = group.planAdherence.code,
)

fun QueuedDiaryEntryPayload.toPendingDto() = PendingDiaryEntryDto(
    clientEntryId = clientEntryId,
    localTimestamp = localTimestamp,
    provenance = provenance,
    referenceFoodId = referenceFoodId,
    portionGrams = portionGrams,
    confidence = confidence,
    confirmed = confirmed,
    planAdherence = planAdherence,
)

/** `null` si lo guardado ya no se puede leer como una comida válida. */
fun QueuedDiaryEntryPayload.toDomainOrNull(rejectionCode: String?): PendingDiaryEntry? {
    val timestamp = localTimestamp.toOffsetDateTimeOrNull() ?: return null
    return try {
        PendingDiaryEntry(
            clientEntryId = ClientEntryId(clientEntryId),
            localTimestamp = LocalTimestamp.restore(timestamp),
            provenance = EntryProvenance.fromCode(provenance) ?: EntryProvenance.MANUAL,
            foodName = foodName,
            grams = portionGrams,
            confidence = confidence,
            confirmed = confirmed,
            planAdherence = PlanAdherence.fromCode(planAdherence),
            rejectionCode = rejectionCode,
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

// ----- Foto -----

/** @throws IllegalArgumentException si el análisis rompe una invariante (confianza fuera de 0–1, gramos ≤ 0…). */
fun MealPhotoAnalysisDto.toDomain(): MealPhotoAnalysis = MealPhotoAnalysis(
    id = MealPhotoAnalysisId(analysisId),
    food = MealFood(referenceFoodId = referenceFoodId, name = foodName.trim()),
    estimatedGrams = estimatedGrams,
    confidence = confidence,
    alternatives = alternatives.take(MealPhotoAnalysis.MAX_ALTERNATIVES).mapNotNull { alternative ->
        val name = alternative.name.trim()
        if (name.isEmpty() || alternative.grams <= 0) return@mapNotNull null
        MealPhotoAlternative(
            name = name,
            grams = alternative.grams,
            food = alternative.referenceFoodId?.takeIf { it > 0 }?.let { MealFood(it, name) },
        )
    },
    expiresAt = requireNotNull(expiresAt.toInstantOrNull()) { "Unreadable expiresAt" },
)

// ----- Ideas -----

fun MealIdeasDto.toDomain(): MealIdeas = MealIdeas(
    localDate = requireNotNull(localDate.toLocalDateOrNull()) { "Unreadable localDate" },
    remainingKcal = remainingKcal.coerceAtLeast(0.0),
    restrictions = restrictions.mapNotNull(RestrictionCode::fromCode).distinct(),
    ideas = ideas.distinctBy { it.mealIdeaId }.mapNotNull { idea ->
        try {
            MealIdea(
                id = idea.mealIdeaId,
                name = idea.name.trim(),
                energyKcal = idea.energyKcal,
                proteinG = idea.proteinG,
                carbG = idea.carbG,
                fatG = idea.fatG,
                ingredients = idea.ingredients.mapNotNull { ingredient ->
                    val name = ingredient.name.trim()
                    if (name.isEmpty()) return@mapNotNull null
                    val foodId = ingredient.referenceFoodId?.takeIf { ingredient.resolved && it > 0 }
                    MealIdeaIngredient(
                        name = name,
                        grams = ingredient.grams.coerceAtLeast(0.0),
                        food = foodId?.let { MealFood(it, ingredient.catalogName?.trim()?.takeIf(String::isNotEmpty) ?: name) },
                    )
                },
                why = idea.why.trim(),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    },
)

private const val ORIGIN_MEAL_IDEA = "MealIdea"
private const val CONFIRMATION_AS_PROPOSED = "AsProposed"
private const val CONFIRMATION_ADJUSTED = "Adjusted"
