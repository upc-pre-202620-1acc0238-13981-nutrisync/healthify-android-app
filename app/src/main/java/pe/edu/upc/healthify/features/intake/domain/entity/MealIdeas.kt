package pe.edu.upc.healthify.features.intake.domain.entity

import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import java.time.LocalDate

/**
 * «Ideas para hoy» (`MealIdeasResource`, IA-3): 2 o 3 ideas que caben en lo que le queda al paciente hoy y respetan
 * las restricciones de su plan. El texto de la IA ([MealIdea.why], [disclaimer]) ya llega en el idioma del lector.
 *
 * @param restrictions las restricciones del plan que se respetaron (códigos conocidos; los demás se omiten).
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

    /** Los ingredientes que el catálogo resolvió: son los que se registran (FC-2). */
    val loggableIngredients: List<MealIdeaIngredient> get() = ingredients.filter { it.isLoggable }

    /** Los que no están en el catálogo: se excluyen del registro y se avisa (PT14.5). */
    val excludedIngredients: List<MealIdeaIngredient> get() = ingredients.filterNot { it.isLoggable }

    val canBeLogged: Boolean get() = loggableIngredients.isNotEmpty()
}

/**
 * @param food el alimento del catálogo, o `null` si `resolved = false` (la IA lo nombró pero el catálogo no lo tiene).
 */
data class MealIdeaIngredient(val name: String, val grams: Double, val food: MealFood?) {
    init {
        require(name.isNotBlank()) { "An ingredient needs a name" }
        require(grams >= 0) { "Grams cannot be negative" }
    }

    /** Resuelto y con una porción que se puede registrar. */
    val isLoggable: Boolean get() = food != null && PortionGrams.ofOrNull(grams) != null
}
