package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BirthDate
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ClinicalRange
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.HeightCm
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.NumberInput
import java.time.LocalDate

/**
 * `PatientBaselineResource`: the baseline, recorded only once. Only the practitioner sees it.
 *
 * @param ageYears age as of today according to the backend ("updates itself").
 */
data class PatientBaseline(
    val birthDate: LocalDate,
    val ageYears: Int,
    val sex: BiologicalSex,
    val heightCm: Double,
    val conditions: Set<MedicalCondition>,
) {
    init {
        require(ageYears >= 0) { "Age cannot be negative" }
        require(heightCm > 0) { "Height must be positive" }
    }
}

/** What `POST`/`PUT /patients/{pid}/baseline` sends. No condition checked = no medical history. */
data class NewBaseline(
    val birthDate: BirthDate,
    val sex: BiologicalSex,
    val height: HeightCm,
    val conditions: Set<MedicalCondition>,
)

/** What the baseline form is missing before it can be saved. */
enum class BaselineProblem { BIRTH_DATE_REQUIRED, BIRTH_DATE_IMPLAUSIBLE, SEX_REQUIRED, HEIGHT_REQUIRED, HEIGHT_OUT_OF_RANGE }

sealed interface BaselineValidation {
    data class Valid(val baseline: NewBaseline) : BaselineValidation
    data class Invalid(val problems: Set<BaselineProblem>) : BaselineValidation
}

/** Validates the baseline on the phone with the backend's rules (`InvalidBirthDate`, `InvalidHeight`). */
fun validateBaseline(
    birthDate: LocalDate?,
    sex: BiologicalSex?,
    heightText: String,
    conditions: Set<MedicalCondition>,
    today: LocalDate,
): BaselineValidation {
    val problems = mutableSetOf<BaselineProblem>()
    when {
        birthDate == null -> problems += BaselineProblem.BIRTH_DATE_REQUIRED
        !BirthDate.isPlausible(birthDate, today) -> problems += BaselineProblem.BIRTH_DATE_IMPLAUSIBLE
    }
    if (sex == null) problems += BaselineProblem.SEX_REQUIRED
    val height = when (val input = ClinicalRange.HEIGHT_CM.parse(heightText)) {
        is NumberInput.Valid -> input.value
        NumberInput.Empty -> null.also { problems += BaselineProblem.HEIGHT_REQUIRED }
        NumberInput.OutOfRange -> null.also { problems += BaselineProblem.HEIGHT_OUT_OF_RANGE }
    }
    if (problems.isNotEmpty() || birthDate == null || sex == null || height == null) {
        return BaselineValidation.Invalid(problems)
    }
    return BaselineValidation.Valid(NewBaseline(BirthDate(birthDate, today), sex, HeightCm(height), conditions))
}
