package pe.edu.upc.healthify.features.intake.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.intake.domain.entity.MealIdeas
import pe.edu.upc.healthify.features.intake.domain.repository.MealIdeasRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.intake.infrastructure.remote.MealIdeasService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.dto.GenerateMealIdeasRequestDto
import java.time.LocalDate
import javax.inject.Inject

/** IA-3 · sin caché en el teléfono: las ideas dependen de lo que queda hoy y solo se generan con conexión (PT14.4.O). */
class MealIdeasRepositoryImpl @Inject constructor(
    private val service: MealIdeasService,
) : MealIdeasRepository {

    override suspend fun generate(
        patientId: PatientId,
        localDate: LocalDate,
        excludeIdeaIds: List<String>,
    ): Result<MealIdeas> {
        val body = GenerateMealIdeasRequestDto(localDate = localDate.toString(), excludeIdeaIds = excludeIdeaIds.ifEmpty { null })
        val dto = apiCall { service.generate(patientId.value, body) }.getOrElse { return Result.failure(it) }
        return try {
            Result.success(dto.toDomain())
        } catch (_: IllegalArgumentException) {
            domainFailure(DomainError.Unexpected(CODE_INVALID_IDEAS))
        }
    }

    private companion object {
        const val CODE_INVALID_IDEAS = "INVALID_MEAL_IDEAS"
    }
}
