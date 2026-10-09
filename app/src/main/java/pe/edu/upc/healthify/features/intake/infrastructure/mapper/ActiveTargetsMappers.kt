package pe.edu.upc.healthify.features.intake.infrastructure.mapper

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.ActiveTargetsDto
import java.time.Instant

/**
 * `ActiveTargetsResource` → dominio.
 * - Indicaciones: `guidelineItems` si llega (NC-6); si no (backend previo), cada texto de `guidelines` es una
 *   indicación propia, salvo que sea un código del catálogo.
 * - Restricciones: primero los códigos (traducibles), después `legacyRestrictions` tal cual.
 *
 * @throws IllegalArgumentException si el recurso rompe una invariante (versión ≤ 0, energía ≤ 0, fecha ilegible).
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

fun ActiveTargetsDto.toEntity(json: Json, savedAt: Instant): ActiveTargetsCacheEntity =
    ActiveTargetsCacheEntity(
        patientId = patientId,
        payloadJson = json.encodeToString(ActiveTargetsDto.serializer(), this),
        savedAtEpochMillis = savedAt.toEpochMilli(),
    )

/** La copia guardada, o `null` si ya no se puede leer (formato viejo o dañado): se trata como «sin copia». */
fun ActiveTargetsCacheEntity.toDtoOrNull(json: Json): ActiveTargetsDto? =
    try {
        json.decodeFromString(ActiveTargetsDto.serializer(), payloadJson)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
