package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealPhotoAnalysisId
import java.time.Instant

/**
 * What the server AI estimated from the photo (`MealPhotoAnalysisResource`). It is a **proposal**, never
 * intake: nothing is stored in the diary until the patient confirms (*Estimate Stored As Proposal Only*).
 *
 * @param confidence always present, between 0 and 1 (*Confidence Always Attached*).
 * @param expiresAt the analysis expires after 24 h (`422 MealPhotoAnalysisExpired`).
 * @param planFit the "Was it in your plan?" suggestion for the dish; `null` if there is none (no published plan).
 */
data class MealPhotoAnalysis(
    val id: MealPhotoAnalysisId,
    val food: MealFood,
    val estimatedGrams: Double,
    val confidence: Double,
    val alternatives: List<MealPhotoAlternative>,
    val expiresAt: Instant,
    val planFit: PlanFit? = null,
) {
    init {
        require(estimatedGrams > 0) { "An estimate needs grams" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        require(alternatives.size <= MAX_ALTERNATIVES) { "At most $MAX_ALTERNATIVES alternatives" }
    }

    /** The alternatives that can be picked: only the ones the catalog resolved. */
    val selectableAlternatives: List<MealPhotoAlternative> get() = alternatives.filter { it.food != null }

    companion object {
        const val MAX_ALTERNATIVES = 3
    }
}

/**
 * Another possible dish. [food] is `null` if it is not in the local catalog (it is shown, but cannot be picked).
 * [planFit] is its own plan suggestion, `null` if there is none.
 */
data class MealPhotoAlternative(
    val name: String,
    val grams: Double,
    val food: MealFood?,
    val planFit: PlanFit? = null,
) {
    init {
        require(name.isNotBlank()) { "An alternative needs a name" }
        require(grams > 0) { "An alternative needs grams" }
    }
}

/**
 * A photo taken offline that waits to be analyzed when the network returns (capture still works offline and
 * stays pending). It lives in `cacheDir` and is deleted once analyzed or discarded.
 *
 * @param capturedAt when it was taken: it is the proposed "When did you eat?" when logging it.
 */
data class PendingMealPhoto(
    val id: String,
    val filePath: String,
    val capturedAt: LocalTimestamp,
) {
    init {
        require(id.isNotBlank() && filePath.isNotBlank()) { "A pending photo needs an id and a file" }
    }
}
