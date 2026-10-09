package pe.edu.upc.healthify.features.nutritionalcare.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.GuidelineCode
import pe.edu.upc.healthify.features.intake.domain.valueobject.Macronutrient
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode

class PlanTextsTest {

    @Test
    fun `every guideline code has its own translated text`() {
        val texts = GuidelineCode.entries.map { it.labelRes }
        assertEquals(GuidelineCode.entries.size, texts.toSet().size)
        assertEquals(UiText.of(R.string.guideline_reduce_salt), PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT).toUiText())
        assertEquals(
            UiText.of(R.string.guideline_drink_2l_water),
            PlanGuideline.Catalog(GuidelineCode.DRINK_2L_WATER).toUiText(),
        )
    }

    @Test
    fun `every restriction code has its own translated text`() {
        val texts = RestrictionCode.entries.map { it.labelRes }
        assertEquals(RestrictionCode.entries.size, texts.toSet().size)
        assertEquals(
            UiText.of(R.string.restriction_shellfish_free),
            DietaryRestriction.Catalog(RestrictionCode.SHELLFISH_FREE).toUiText(),
        )
    }

    @Test
    fun `text written by the nutritionist is shown as written, never translated`() {
        assertEquals(UiText.Raw("Caminar 20 min"), PlanGuideline.Custom("Caminar 20 min").toUiText())
        assertEquals(UiText.Raw("Sin ají"), DietaryRestriction.Legacy("Sin ají").toUiText())
    }

    @Test
    fun `guideline and restriction changes nest the translated item in the sentence`() {
        assertEquals(
            UiText.of(R.string.plan_change_guideline_added, UiText.of(R.string.guideline_reduce_salt)),
            PlanChange.GuidelineAdded(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT)).toUiText(),
        )
        assertEquals(
            UiText.of(R.string.plan_change_guideline_removed, UiText.Raw("Caminar 20 min")),
            PlanChange.GuidelineRemoved(PlanGuideline.Custom("Caminar 20 min")).toUiText(),
        )
        assertEquals(
            UiText.of(R.string.plan_change_restriction_added, UiText.of(R.string.restriction_vegan)),
            PlanChange.RestrictionAdded(DietaryRestriction.Catalog(RestrictionCode.VEGAN)).toUiText(),
        )
        assertEquals(
            UiText.of(R.string.plan_change_restriction_removed, UiText.Raw("Sin ají")),
            PlanChange.RestrictionRemoved(DietaryRestriction.Legacy("Sin ají")).toUiText(),
        )
    }

    @Test
    fun `target changes keep the numbers for locale formatting`() {
        assertEquals(
            UiText.of(R.string.plan_change_energy, 1796.0, 1650.0),
            PlanChange.EnergyChanged(fromKcal = 1796.0, toKcal = 1650.0).toUiText(),
        )
        assertEquals(
            UiText.of(R.string.plan_change_macro, UiText.of(R.string.macro_protein_inline), 80.0, 90.0),
            PlanChange.MacroChanged(Macronutrient.PROTEIN, fromG = 80.0, toG = 90.0).toUiText(),
        )
    }

    @Test
    fun `no target changes is the secondary line`() {
        assertEquals(UiText.of(R.string.plan_change_no_target_changes), PlanChange.NoTargetChanges.toUiText())
        assertTrue(PlanChange.NoTargetChanges.isSecondary)
        assertFalse(PlanChange.EnergyChanged(1.0, 2.0).isSecondary)
    }
}
