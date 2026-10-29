package pe.edu.upc.healthify.features.intake.infrastructure.mapper

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.intake.domain.entity.DailyIntake
import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.DailyIntakeDto
import java.time.Instant

/**
 * `ActiveTargetsResource` → domain.
 * - Guidelines: `guidelineItems` if present; otherwise (older backend) each `guidelines` text is its own
 *   guideline, unless it is a catalog code.
 * - Restrictions: the (translatable) codes first, then `legacyRestrictions` as is.
 *
 * @throws IllegalArgumentException if the resource breaks an invariant (version ≤ 0, energy ≤ 0, unreadable date).
 */
fun ActiveTargetsDto.toDomain(): ActiveTargets {
    val guidelineList = guidelineItems?.mapNotNull { PlanGuideline.of(it.code, it.custom) }
        ?: guidelines.mapNotNull { PlanGuideline.of(code = it, custom = it) }
    val restrictionList = restrictions.mapNotNull(DietaryRestriction::ofCode) +
        legacyRestrictions.orEmpty().mapNotNull(DietaryRestriction::legacy)
    return ActiveTargets(
        patientId = PatientId(patientId),
        planVersion = planVersion,
        validFrom = requireNotNull(validFrom.toInstantOrNull()) { "Unreadable validFrom" },
        targets = DailyTargets(energyKcal = energyKcal, proteinG = proteinG, carbG = carbG, fatG = fatG),
        guidelines = guidelineList.distinct(),
        restrictions = restrictionList.distinct(),
        changesFromPrevious = changesFromPrevious.orEmpty().mapNotNull {
            PlanChange.of(it.type, it.code, it.custom, it.macro, it.from, it.to)
        },
        patientMessage = patientMessage?.trim()?.takeIf { it.isNotEmpty() },
    )
}

/** `null` if the resource breaks an invariant (unreadable date or negative values): the gauge uses the targets. */
fun DailyIntakeDto.toDomainOrNull(): DailyIntake? {
    val day = date.toLocalDateOrNull() ?: return null
    return runCatching {
        DailyIntake(
            date = day,
            energyKcal = energyKcal,
            proteinG = proteinG,
            carbG = carbG,
            fatG = fatG,
            entryCount = entryCount,
            remainingEnergyKcal = remainingEnergyKcal,
            remainingProteinG = remainingProteinG,
            remainingCarbG = remainingCarbG,
            remainingFatG = remainingFatG,
        )
    }.getOrNull()
}

fun ActiveTargetsDto.toEntity(json: Json, savedAt: Instant): ActiveTargetsCacheEntity =
    ActiveTargetsCacheEntity(
        patientId = patientId,
        payloadJson = json.encodeToString(ActiveTargetsDto.serializer(), this),
        savedAtEpochMillis = savedAt.toEpochMilli(),
    )

/** The saved copy, or `null` if it can no longer be read (old or damaged format): treated as "no copy". */
fun ActiveTargetsCacheEntity.toDtoOrNull(json: Json): ActiveTargetsDto? =
    try {
        json.decodeFromString(ActiveTargetsDto.serializer(), payloadJson)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
