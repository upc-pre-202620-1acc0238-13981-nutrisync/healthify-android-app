package pe.edu.upc.healthify.features.intake.application.usecase

import pe.edu.upc.healthify.features.intake.domain.entity.MealLogOutcome
import pe.edu.upc.healthify.features.intake.domain.entity.MealPhotoAnalysis
import pe.edu.upc.healthify.features.intake.domain.entity.NewManualMeal
import pe.edu.upc.healthify.features.intake.domain.entity.NewPhotoMeal
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.ClientEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.DiaryEntryId
import pe.edu.upc.healthify.features.intake.domain.valueobject.LocalTimestamp
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.intake.domain.valueobject.PlanAdherence
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import javax.inject.Inject

/** PT10.1 «Registrar» (F17). Sin conexión queda encolada («Pendiente de enviar»). */
class LogManualMealUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long, meal: NewManualMeal): Result<MealLogOutcome> =
        repository.logManualMeal(PatientId(patientUserId), meal)
}

/** PT7 «Sí, es correcto» / PT8 «Confirmar» de una foto (F41 → `photo-logs` con `analysisId`). */
class LogPhotoMealUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long, meal: NewPhotoMeal): Result<MealLogOutcome> =
        repository.logPhotoMeal(PatientId(patientUserId), meal)
}

/** PT7 · salir sin confirmar: la comida queda «Por confirmar» y aún no suma a las metas. */
class KeepPhotoMealUnconfirmedUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(
        patientUserId: Long,
        analysis: MealPhotoAnalysis,
        localTimestamp: LocalTimestamp,
        clientEntryId: ClientEntryId,
    ): Result<MealLogOutcome> =
        repository.keepPhotoMealUnconfirmed(PatientId(patientUserId), analysis, localTimestamp, clientEntryId)
}

/**
 * PT14 «Confirmar porción» → PT8 de una entrada «Por confirmar» (F16). Si el paciente no cambió ni el plato ni los
 * gramos, confirma la propuesta; si cambió algo, la ajusta (la propuesta se conserva al lado).
 */
class ConfirmDiaryEntryUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(
        entryId: DiaryEntryId,
        proposedFood: MealFood,
        proposedGrams: Double,
        food: MealFood,
        portion: PortionGrams,
        planAdherence: PlanAdherence,
    ): Result<Unit> {
        require(planAdherence.isAnswered) { "Confirming needs the plan answer (PlanAdherenceRequired)" }
        val unchanged = food.referenceFoodId == proposedFood.referenceFoodId && portion.value == proposedGrams
        return if (unchanged) {
            repository.confirmEstimate(entryId, planAdherence)
        } else {
            repository.adjustEstimate(entryId, food, portion, planAdherence)
        }
    }
}
