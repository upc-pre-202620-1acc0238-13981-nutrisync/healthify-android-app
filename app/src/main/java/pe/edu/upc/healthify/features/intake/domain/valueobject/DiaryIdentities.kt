package pe.edu.upc.healthify.features.intake.domain.valueobject

import java.util.UUID

@JvmInline
value class DiaryEntryId(val value: Long) {
    init {
        require(value > 0) { "DiaryEntryId must be positive" }
    }
}

/** Id of a photo analysis (`MealPhotoAnalysisResource.analysisId`, Guid; expires after 24 h). */
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
 * A catalog food as the diary uses it: its id (FoodCatalog resolves it) and the name that is shown.
 * Intake does not know the nutrients: the backend takes them from the catalog.
 */
data class MealFood(val referenceFoodId: Long, val name: String) {
    init {
        require(referenceFoodId > 0) { "A food needs its catalog id" }
        require(name.isNotBlank()) { "A food needs a name" }
    }
}

/** How the entry was logged. [OFF_PLAN] only appears on historical rows logged without a food. */
enum class EntryProvenance(val code: String) {
    PHOTO("Photo"),
    MANUAL("Manual"),
    OFF_PLAN("OffPlan"),
    ;

    companion object {
        fun fromCode(code: String?): EntryProvenance? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
