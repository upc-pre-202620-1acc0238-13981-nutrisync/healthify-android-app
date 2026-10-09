package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.util.UUID

@JvmInline
value class DiaryEntryId(val value: Long) {
    init {
        require(value > 0) { "DiaryEntryId must be positive" }
    }
}

/** Id del análisis de una foto (`MealPhotoAnalysisResource.analysisId`, Guid; vence a las 24 h). */
@JvmInline
value class MealPhotoAnalysisId(val value: String) {
    init {
        require(
            try {
                UUID.fromString(value)
                true
            } catch (_: IllegalArgumentException) {
                false
            },
        ) { "MealPhotoAnalysisId must be a UUID" }
    }
}

/**
 * Un alimento del catálogo tal como lo usa el diario: su id (lo resuelve FoodCatalog) y el nombre que se muestra.
 * Intake no conoce los nutrientes: el backend los toma del catálogo.
 */
data class MealFood(val referenceFoodId: Long, val name: String) {
    init {
        require(referenceFoodId > 0) { "A food needs its catalog id" }
        require(name.isNotBlank()) { "A food needs a name" }
    }
}

/** Cómo se registró la entrada. [OFF_PLAN] es solo de filas históricas (antes de IN-1, sin alimento). */
enum class EntryProvenance(val code: String) {
    PHOTO("Photo"),
    MANUAL("Manual"),
    OFF_PLAN("OffPlan"),
    ;

    companion object {
        fun fromCode(code: String?): EntryProvenance? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
