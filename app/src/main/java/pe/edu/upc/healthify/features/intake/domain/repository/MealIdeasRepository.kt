package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/** `POST /patients/{pid}/meal-ideas`. */
interface MealIdeasRepository {

    /**
     * @param localDate the phone's day (`400 InvalidLocalDate` if it is not today ±1).
     * @param excludeIdeaIds "See other ideas" sends the ones already seen.
     */
    suspend fun generate(patientId: PatientId, localDate: LocalDate, excludeIdeaIds: List<String>): Result<MealIdeas>
}
