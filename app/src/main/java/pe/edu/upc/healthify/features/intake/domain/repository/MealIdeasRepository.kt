package pe.edu.upc.healthify.features.intake.domain.repository

import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate

/** IA-3 · `POST /patients/{pid}/meal-ideas`. */
interface MealIdeasRepository {

    /**
     * @param localDate el día del teléfono (`400 InvalidLocalDate` si no es hoy ±1).
     * @param excludeIdeaIds «Ver otras ideas» manda las ya vistas.
     */
    suspend fun generate(patientId: PatientId, localDate: LocalDate, excludeIdeaIds: List<String>): Result<MealIdeas>
}
