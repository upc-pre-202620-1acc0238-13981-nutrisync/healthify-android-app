package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import java.time.LocalDate

/** IN-8 · `GET /patients/{pid}/plan-fit`. Solo con conexión: sin ella no hay sugerencia. */
interface PlanFitRepository {

    /**
     * @param localDate el día de la comida para pesar lo que queda; `null` solo mira las restricciones.
     * Errores: `404 ActiveTargetsCacheNotFound` (sin plan publicado), `422 ReferenceFoodNotResolved`.
     */
    suspend fun check(patientId: PatientId, food: MealFood, portion: PortionGrams, localDate: LocalDate?): Result<PlanFit>
}
