package pe.edu.upc.healthify.features.intake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.upc.healthify.features.intake.domain.entity.ActiveTargetsLookup
import pe.edu.upc.healthify.features.intake.domain.valueobject.DailyTargets
import pe.edu.upc.healthify.features.intake.domain.valueobject.DietaryRestriction
import pe.edu.upc.healthify.features.intake.domain.valueobject.GuidelineCode
import pe.edu.upc.healthify.features.intake.domain.valueobject.Macronutrient
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanChange
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanGuideline
import pe.edu.upc.healthify.features.intake.domain.valueobject.RestrictionCode
import pe.edu.upc.healthify.testing.activeTargets

class IntakeDomainTest {

    @Test(expected = IllegalArgumentException::class)
    fun `a published contract has a positive energy target`() {
        DailyTargets(energyKcal = 0.0, proteinG = 90.0, carbG = 200.0, fatG = 60.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `macro targets cannot be negative`() {
        DailyTargets(energyKcal = 1850.0, proteinG = -1.0, carbG = 200.0, fatG = 60.0)
    }

    @Test
    fun `energy share uses 4-4-9 kcal per gram`() {
        val targets = DailyTargets(energyKcal = 2000.0, proteinG = 100.0, carbG = 250.0, fatG = 50.0)
        assertEquals(0.2, targets.energyShareOf(Macronutrient.PROTEIN), 1e-9)
        assertEquals(0.5, targets.energyShareOf(Macronutrient.CARB), 1e-9)
        assertEquals(0.225, targets.energyShareOf(Macronutrient.FAT), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plan versions are positive`() {
        activeTargets(planVersion = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a blank nutritionist message must be null`() {
        activeTargets(patientMessage = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a cached lookup needs the moment it was saved`() {
        ActiveTargetsLookup(targets = activeTargets(), fromCache = true, savedAt = null)
    }

    @Test
    fun `guideline items read the catalog code first`() {
        assertEquals(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT), PlanGuideline.of("ReduceSalt", null))
        assertEquals(PlanGuideline.Custom("Caminar 20 min"), PlanGuideline.of(null, " Caminar 20 min "))
        // X-2: un código desconocido se muestra tal cual.
        assertEquals(PlanGuideline.Custom("SleepEarly"), PlanGuideline.of("SleepEarly", null))
        assertNull(PlanGuideline.of(null, " "))
    }

    @Test
    fun `restrictions translate known codes and keep legacy text as written`() {
        assertEquals(DietaryRestriction.Catalog(RestrictionCode.GLUTEN_FREE), DietaryRestriction.ofCode("GlutenFree"))
        assertEquals(DietaryRestriction.Legacy("Paleo"), DietaryRestriction.ofCode("Paleo"))
        assertEquals(DietaryRestriction.Legacy("Sin ají"), DietaryRestriction.legacy("Sin ají"))
        assertNull(DietaryRestriction.legacy(""))
    }

    @Test
    fun `plan changes are read by type and incomplete ones are dropped`() {
        assertEquals(
            PlanChange.GuidelineAdded(PlanGuideline.Catalog(GuidelineCode.REDUCE_SALT)),
            PlanChange.of("GuidelineAdded", "ReduceSalt", null, null, null, null),
        )
        assertEquals(
            PlanChange.RestrictionRemoved(DietaryRestriction.Legacy("Sin ají")),
            PlanChange.of("RestrictionRemoved", null, "Sin ají", null, null, null),
        )
        assertEquals(PlanChange.EnergyChanged(1796.0, 1650.0), PlanChange.of("EnergyChanged", null, null, null, 1796.0, 1650.0))
        assertEquals(
            PlanChange.MacroChanged(Macronutrient.FAT, 70.0, 60.0),
            PlanChange.of("MacroChanged", null, null, "Fat", 70.0, 60.0),
        )
        assertEquals(PlanChange.NoTargetChanges, PlanChange.of("NoTargetChanges", null, null, null, null, null))
        assertNull(PlanChange.of("EnergyChanged", null, null, null, null, 1650.0))
        assertNull(PlanChange.of("MacroChanged", null, null, "Fiber", 1.0, 2.0))
        assertNull(PlanChange.of("SomethingNew", "x", null, null, null, null))
    }
}
