package pe.edu.upc.healthify.features.intake.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.repository.PlanFitRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.infrastructure.mapper.toDomainOrNull
import pe.edu.upc.healthify.features.intake.infrastructure.remote.PlanFitService
import java.time.LocalDate
import javax.inject.Inject

/** IN-8 · sin caché: la sugerencia depende de lo que queda hoy y solo existe con conexión. */
class PlanFitRepositoryImpl @Inject constructor(
    private val service: PlanFitService,
) : PlanFitRepository {

    override suspend fun check(
        patientId: PatientId,
        food: MealFood,
        portion: PortionGrams,
        localDate: LocalDate?,
    ): Result<PlanFit> {
        val dto = apiCall { service.check(patientId.value, food.referenceFoodId, portion.value, localDate?.toString()) }
            .getOrElse { return Result.failure(it) }
        return dto.toDomainOrNull()?.let { Result.success(it) } ?: domainFailure(DomainError.Unexpected(CODE_INVALID_PLAN_FIT))
    }

    private companion object {
        const val CODE_INVALID_PLAN_FIT = "INVALID_PLAN_FIT"
    }
}
