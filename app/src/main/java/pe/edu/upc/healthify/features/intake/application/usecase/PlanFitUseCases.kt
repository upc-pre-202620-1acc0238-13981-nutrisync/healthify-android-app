package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.repository.PlanFitRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

/**
 * IN-8 · la sugerencia de «¿Estaba en tu plan?» para un plato y una porción. El servidor solo pesa lo que queda del
 * día de hoy (±1); para una comida de otro día se piden solo las restricciones.
 */
class CheckPlanFitUseCase @Inject constructor(
    private val repository: PlanFitRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        food: MealFood,
        portion: PortionGrams,
        mealDate: LocalDate,
        today: LocalDate,
    ): Result<PlanFit> {
        val localDate = mealDate.takeIf { abs(it.toEpochDay() - today.toEpochDay()) <= MAX_DAYS_FROM_TODAY }
        return repository.check(PatientId(patientUserId), food, portion, localDate)
    }

    private companion object {
        const val MAX_DAYS_FROM_TODAY = 1
    }
}
