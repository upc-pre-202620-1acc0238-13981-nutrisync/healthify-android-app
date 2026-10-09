package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.GuidelineCode
import pe.edu.upc.healthify.features.intake.domain.valueobject.Macronutrient
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode

/*
 * Códigos del plan (NC-6/NC-8) → textos de strings.xml en el idioma del lector. Los textos escritos por el
 * nutricionista (indicación propia, restricción legada) se devuelven tal cual con UiText.Raw: nunca se traducen.
 */

fun PlanGuideline.toUiText(): UiText = when (this) {
    is PlanGuideline.Catalog -> UiText.of(code.labelRes)
    is PlanGuideline.Custom -> UiText.Raw(text)
}

fun DietaryRestriction.toUiText(): UiText = when (this) {
    is DietaryRestriction.Catalog -> UiText.of(code.labelRes)
    is DietaryRestriction.Legacy -> UiText.Raw(text)
}

fun PlanChange.toUiText(): UiText = when (this) {
    is PlanChange.GuidelineAdded -> UiText.of(R.string.plan_change_guideline_added, guideline.toUiText())
    is PlanChange.GuidelineRemoved -> UiText.of(R.string.plan_change_guideline_removed, guideline.toUiText())
    is PlanChange.RestrictionAdded -> UiText.of(R.string.plan_change_restriction_added, restriction.toUiText())
    is PlanChange.RestrictionRemoved -> UiText.of(R.string.plan_change_restriction_removed, restriction.toUiText())
    is PlanChange.EnergyChanged -> UiText.of(R.string.plan_change_energy, fromKcal, toKcal)
    is PlanChange.MacroChanged -> UiText.of(R.string.plan_change_macro, UiText.of(macro.inlineRes), fromG, toG)
    PlanChange.NoTargetChanges -> UiText.of(R.string.plan_change_no_target_changes)
}

/** «Las metas … no cambiaron» va en texto secundario, como en el frame de PT4. */
val PlanChange.isSecondary: Boolean get() = this == PlanChange.NoTargetChanges

val GuidelineCode.labelRes: Int
    get() = when (this) {
        GuidelineCode.PRIORITIZE_VEGETABLES -> R.string.guideline_prioritize_vegetables
        GuidelineCode.DRINK_2L_WATER -> R.string.guideline_drink_2l_water
        GuidelineCode.AVOID_SUGARY_DRINKS -> R.string.guideline_avoid_sugary_drinks
        GuidelineCode.PROTEIN_AT_BREAKFAST -> R.string.guideline_protein_at_breakfast
        GuidelineCode.REDUCE_SALT -> R.string.guideline_reduce_salt
        GuidelineCode.EAT_EVERY_3_TO_4_HOURS -> R.string.guideline_eat_every_3_to_4_hours
        GuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER -> R.string.guideline_protein_and_vegetables_at_dinner
    }

val RestrictionCode.labelRes: Int
    get() = when (this) {
        RestrictionCode.LACTOSE_FREE -> R.string.restriction_lactose_free
        RestrictionCode.GLUTEN_FREE -> R.string.restriction_gluten_free
        RestrictionCode.VEGAN -> R.string.restriction_vegan
        RestrictionCode.VEGETARIAN -> R.string.restriction_vegetarian
        RestrictionCode.TREE_NUT_FREE -> R.string.restriction_tree_nut_free
        RestrictionCode.SHELLFISH_FREE -> R.string.restriction_shellfish_free
        RestrictionCode.KOSHER -> R.string.restriction_kosher
        RestrictionCode.HALAL -> R.string.restriction_halal
    }

/** Nombre del macronutriente dentro de una frase («Tu meta de proteína pasó de…»). */
val Macronutrient.inlineRes: Int
    get() = when (this) {
        Macronutrient.PROTEIN -> R.string.macro_protein_inline
        Macronutrient.CARB -> R.string.macro_carb_inline
        Macronutrient.FAT -> R.string.macro_fat_inline
    }
