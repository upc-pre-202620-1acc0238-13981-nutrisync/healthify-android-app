package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import java.time.LocalDate

/**
 * "Ideas for today" (`MealIdeasResource`): 2 or 3 ideas that fit what the patient has left today and respect
 * their plan's restrictions. The AI text ([MealIdea.why], [disclaimer]) already arrives in the reader's language.
 *
 * @param restrictions the plan restrictions that were respected (known codes; the rest are skipped).
 */
data class MealIdeas(
    val localDate: LocalDate,
    val remainingKcal: Double,
    val restrictions: List<RestrictionCode>,
    val ideas: List<MealIdea>,
) {
    init {
        require(remainingKcal >= 0) { "Remaining energy cannot be negative" }
        require(ideas.map { it.id }.toSet().size == ideas.size) { "Each idea appears once" }
    }
}

data class MealIdea(
    val id: String,
    val name: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val ingredients: List<MealIdeaIngredient>,
    val why: String,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank()) { "An idea needs an id and a name" }
        require(energyKcal >= 0 && proteinG >= 0 && carbG >= 0 && fatG >= 0) { "Nutrients cannot be negative" }
    }

    /** The ingredients the catalog resolved: these are the ones that get logged. */
    val loggableIngredients: List<MealIdeaIngredient> get() = ingredients.filter { it.isLoggable }

    /** The ones not in the catalog: they are left out of the log and the patient is warned. */
    val excludedIngredients: List<MealIdeaIngredient> get() = ingredients.filterNot { it.isLoggable }

    val canBeLogged: Boolean get() = loggableIngredients.isNotEmpty()
}

/**
 * @param food the catalog food, or `null` if `resolved = false` (the AI named it but the catalog does not have it).
 */
data class MealIdeaIngredient(val name: String, val grams: Double, val food: MealFood?) {
    init {
        require(name.isNotBlank()) { "An ingredient needs a name" }
        require(grams >= 0) { "Grams cannot be negative" }
    }

    /** Resolved and with a portion that can be logged. */
    val isLoggable: Boolean get() = food != null && PortionGrams.ofOrNull(grams) != null
}
