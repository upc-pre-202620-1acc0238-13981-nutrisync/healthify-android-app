package pe.edu.upc.healthify.features.nutritionalcare.domain.entity

import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BirthDate
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ClinicalRange
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.HeightCm
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.NumberInput
import java.time.LocalDate

/**
 * `PatientBaselineResource` (NC-1): los datos base que se registran una sola vez. Solo el profesional los ve.
 *
 * @param ageYears edad cumplida hoy según el backend («se actualiza sola»).
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

/** EV-1 · lo que se envía en `POST`/`PUT /patients/{pid}/baseline`. Ningún antecedente marcado = sin antecedentes. */
data class NewBaseline(
    val birthDate: BirthDate,
    val sex: BiologicalSex,
    val height: HeightCm,
    val conditions: Set<MedicalCondition>,
)

/** Qué le falta a EV-1 para poder guardarse. */
enum class BaselineProblem { BIRTH_DATE_REQUIRED, BIRTH_DATE_IMPLAUSIBLE, SEX_REQUIRED, HEIGHT_REQUIRED, HEIGHT_OUT_OF_RANGE }

sealed interface BaselineValidation {
    data class Valid(val baseline: NewBaseline) : BaselineValidation
    data class Invalid(val problems: Set<BaselineProblem>) : BaselineValidation
}

/** Valida EV-1 en el teléfono con las mismas reglas del backend (`InvalidBirthDate`, `InvalidHeight`). */
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
