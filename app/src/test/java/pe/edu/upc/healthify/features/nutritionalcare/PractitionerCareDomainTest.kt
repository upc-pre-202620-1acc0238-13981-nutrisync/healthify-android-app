package pe.edu.upc.healthify.features.nutritionalcare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.FieldProblem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementForm
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MeasurementValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.OverrideValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validate
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validateBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.validateOverride
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BirthDate
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BodyMassIndex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ClinicalRange
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.CustomGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DecimalText
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.HeightCm
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.NumberInput
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.OverrideReason
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SuggestionSource
import pe.edu.upc.healthify.testing.savedMeasurement
import java.time.LocalDate

class PractitionerCareDomainTest {

    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun `decimal text accepts comma or dot and rejects anything else`() {
        assertEquals(74.2, DecimalText.parse("74,2")!!, 0.0)
        assertEquals(74.2, DecimalText.parse(" 74.2 ")!!, 0.0)
        assertNull(DecimalText.parse("74.2.1"))
        assertNull(DecimalText.parse("-3"))
        assertNull(DecimalText.parse("abc"))
    }

    @Test
    fun `clinical ranges follow the backend plausibility`() {
        assertEquals(NumberInput.Empty, ClinicalRange.WEIGHT_KG.parse(""))
        assertEquals(NumberInput.OutOfRange, ClinicalRange.WEIGHT_KG.parse("351"))
        assertEquals(NumberInput.Valid(20.0), ClinicalRange.WEIGHT_KG.parse("20"))
        assertEquals(NumberInput.OutOfRange, ClinicalRange.MEALS_PER_DAY.parse("2.5"))
        assertEquals(NumberInput.Valid(168.5), ClinicalRange.HEIGHT_CM.parse("168,46"))
        assertThrows(IllegalArgumentException::class.java) { HeightCm(49.9) }
    }

    @Test
    fun `birth date must be between 1 and 120 years ago and the age is exact`() {
        assertTrue(BirthDate.isPlausible(LocalDate.of(1995, 2, 15), today))
        assertFalse(BirthDate.isPlausible(today.minusMonths(6), today))
        assertFalse(BirthDate.isPlausible(today.minusYears(121), today))
        assertEquals(31, BirthDate(LocalDate.of(1995, 2, 15), today).ageYears)
        assertEquals(30, BirthDate(LocalDate.of(1995, 10, 8), today).ageYears)
    }

    @Test
    fun `bmi is weight over height squared with one decimal and the WHO category`() {
        val bmi = BodyMassIndex.of(74.2, 168.0)
        assertEquals(26.3, bmi.value, 0.0)
        assertEquals(DiagnosisCode.OVERWEIGHT_GRADE_I, bmi.category)
        assertEquals(DiagnosisCode.NORMAL_WEIGHT, DiagnosisCode.forBmi(24.9))
        assertEquals(DiagnosisCode.OBESITY_GRADE_III, DiagnosisCode.forBmi(40.0))
        assertEquals(DiagnosisCode.UNDERWEIGHT, DiagnosisCode.forBmi(18.4))
    }

    @Test
    fun `baseline validation reports every missing field and accepts no medical history`() {
        val invalid = validateBaseline(null, null, "", emptySet(), today) as BaselineValidation.Invalid
        assertEquals(
            setOf(BaselineProblem.BIRTH_DATE_REQUIRED, BaselineProblem.SEX_REQUIRED, BaselineProblem.HEIGHT_REQUIRED),
            invalid.problems,
        )

        val outOfRange = validateBaseline(today.minusDays(3), BiologicalSex.MALE, "300", emptySet(), today)
        assertEquals(
            setOf(BaselineProblem.BIRTH_DATE_IMPLAUSIBLE, BaselineProblem.HEIGHT_OUT_OF_RANGE),
            (outOfRange as BaselineValidation.Invalid).problems,
        )

        val valid = validateBaseline(LocalDate.of(1995, 2, 15), BiologicalSex.FEMALE, "168", emptySet(), today)
        val baseline = (valid as BaselineValidation.Valid).baseline
        assertEquals(168.0, baseline.height.value, 0.0)
        assertTrue(baseline.conditions.isEmpty())
    }

    @Test
    fun `measurement needs weight, a protocol check and the activity, the rest is optional`() {
        val empty = MeasurementForm().validate() as MeasurementValidation.Invalid
        assertEquals(
            mapOf(
                MeasurementField.WEIGHT to FieldProblem.REQUIRED,
                MeasurementField.PROTOCOL to FieldProblem.REQUIRED,
                MeasurementField.ACTIVITY to FieldProblem.REQUIRED,
            ),
            empty.problems,
        )

        val form = MeasurementForm(
            weight = "74,2",
            waist = "88",
            protocolChecks = setOf(ProtocolCheck.FASTING),
            activityLevel = ActivityLevel.MODERATE,
            mealsPerDay = "4",
            glucose = "700",
        )
        val withBadLab = form.validate() as MeasurementValidation.Invalid
        assertEquals(mapOf(MeasurementField.GLUCOSE to FieldProblem.OUT_OF_RANGE), withBadLab.problems)

        val valid = form.copy(glucose = "").validate() as MeasurementValidation.Valid
        assertEquals(74.2, valid.measurement.weightKg, 0.0)
        assertEquals(4, valid.measurement.habits?.mealsPerDay)
        assertNull(valid.measurement.biochemistry)
    }

    @Test
    fun `a measurement equal to the saved one is recognised so it is not sent again`() {
        val same = NewMeasurement(74.2, 88.0, null, setOf(ProtocolCheck.FASTING, ProtocolCheck.NO_SHOES), ActivityLevel.MODERATE, null, null)
        assertTrue(same.sameAs(savedMeasurement()))
        assertFalse(same.copy(weightKg = 74.0).sameAs(savedMeasurement()))
        assertThrows(IllegalArgumentException::class.java) { same.copy(protocolChecks = emptySet()) }
    }

    @Test
    fun `own values require a reason`() {
        val noReason = validateOverride("1700", "120", "180", "55", "  ") as OverrideValidation.Invalid
        assertEquals(setOf(OverrideField.REASON), noReason.problems)

        val badEnergy = validateOverride("0", "120", "x", "55", "Ajuste") as OverrideValidation.Invalid
        assertEquals(setOf(OverrideField.ENERGY, OverrideField.CARB), badEnergy.problems)

        val valid = validateOverride("1700", "120", "180", "55", " Su ritmo de trabajo ") as OverrideValidation.Valid
        assertEquals("Su ritmo de trabajo", valid.prescription.reason.value)
        assertEquals(1700.0, valid.prescription.targets.energyKcal, 0.0)
        assertThrows(IllegalArgumentException::class.java) { OverrideReason("") }
    }

    @Test
    fun `macro shares follow 4-4-9 kcal per gram`() {
        val targets = Targets(1796.0, 119.0, 195.0, 60.0)
        assertEquals(27, targets.proteinPercent)
        assertEquals(43, targets.carbPercent)
        assertEquals(30, targets.fatPercent)
    }

    @Test
    fun `only a real AI generation is recorded as an accepted suggestion`() {
        val ai = DiagnosisSuggestion(77, DiagnosisCode.OVERWEIGHT_GRADE_I, "IMC 26.3", isFromAi = true, disclaimer = "")
        assertEquals(
            DiagnosisChoice.AcceptAiSuggestion(DiagnosisCode.OVERWEIGHT_GRADE_I, 77, "IMC 26.3"),
            DiagnosisChoice.accepting(ai),
        )
        val rule = ai.copy(aiGenerationId = null, isFromAi = false)
        assertEquals(DiagnosisChoice.Selected(DiagnosisCode.OVERWEIGHT_GRADE_I, "IMC 26.3"), DiagnosisChoice.accepting(rule))
        assertEquals(SuggestionSource.RULE, SuggestionSource.fromCode("Something"))
    }

    @Test
    fun `publication limits custom guidelines, their length and the message`() {
        assertThrows(IllegalArgumentException::class.java) { CustomGuideline("ab") }
        assertThrows(IllegalArgumentException::class.java) { CustomGuideline("x".repeat(141)) }
        assertThrows(IllegalArgumentException::class.java) {
            Publication(emptySet(), emptySet(), List(6) { CustomGuideline("Indicación $it") }, null)
        }
        assertNull(PatientMessage.ofOptional("   "))
        assertThrows(IllegalArgumentException::class.java) { PatientMessage("x".repeat(501)) }
    }

    @Test
    fun `idempotency keys are visible ascii and random ones are unique`() {
        val first = IdempotencyKey.random()
        assertTrue(first.value.length <= IdempotencyKey.MAX_LENGTH)
        assertFalse(first == IdempotencyKey.random())
        assertThrows(IllegalArgumentException::class.java) { IdempotencyKey("con espacio") }
        assertThrows(IllegalArgumentException::class.java) { IdempotencyKey("") }
    }

    @Test
    fun `plan history puts the newest version first and finds the active one`() {
        val history = PlanHistory.of(
            listOf(1, 3, 2).map { version ->
                pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NutritionPlanVersion(
                    version = version,
                    isActive = version == 3,
                    publishedAt = java.time.Instant.parse("2026-09-04T15:00:00Z"),
                    targets = Targets(1800.0, 110.0, 200.0, 60.0),
                    guidelines = emptyList(),
                    restrictions = emptyList(),
                    patientMessage = null,
                )
            },
        )
        assertEquals(listOf(3, 2, 1), history.versions.map { it.version })
        assertEquals(3, history.active?.version)
        assertEquals(MedicalCondition.GOUT, MedicalCondition.fromCode("gout"))
    }
}
