package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.LocalDate

/** `GET /patients/{pid}/plan-fit`. Online only: without a connection there is no suggestion. */
interface PlanFitRepository {

    /**
     * @param localDate the day of the meal, to weigh what is left; `null` only looks at the restrictions.
     * Errors: `404 ActiveTargetsCacheNotFound` (no published plan), `422 ReferenceFoodNotResolved`.
     */
    suspend fun check(patientId: PatientId, food: MealFood, portion: PortionGrams, localDate: LocalDate?): Result<PlanFit>
}
