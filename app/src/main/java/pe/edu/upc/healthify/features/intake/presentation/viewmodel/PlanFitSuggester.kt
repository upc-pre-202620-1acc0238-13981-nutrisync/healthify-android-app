package pe.edu.upc.healthify.features.intake.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.edu.upc.healthify.features.intake.application.usecase.CheckPlanFitUseCase
import pe.edu.upc.healthify.features.intake.domain.entity.PlanFit
import pe.edu.upc.healthify.features.intake.domain.valueobject.MealFood
import pe.edu.upc.healthify.features.intake.domain.valueobject.PortionGrams
import pe.edu.upc.healthify.features.intake.presentation.state.MealFormState
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * IN-8 · pide la sugerencia de «¿Estaba en tu plan?» de PT8 y PT10.1 cuando cambian el plato, la porción o el día,
 * con un respiro para no pedirla en cada tecla. Solo vale la última respuesta. Sin conexión o sin plan publicado no
 * hay sugerencia (`null`) y el paciente responde como siempre. Uno por ViewModel (no es singleton).
 */
class PlanFitSuggester @Inject constructor(
    private val checkPlanFit: CheckPlanFitUseCase,
    private val clock: Clock,
) {
    private var job: Job? = null

    /**
     * Sin plato no hay sugerencia ([onResult] con `null`); con una porción que todavía no es válida (el paciente está
     * escribiendo) se conserva la que había.
     */
    fun request(
        scope: CoroutineScope,
        patientUserId: Long,
        food: MealFood?,
        form: MealFormState,
        onResult: (PlanFit?) -> Unit,
    ) {
        job?.cancel()
        if (food == null) {
            onResult(null)
            return
        }
        val portion = PortionGrams.parseOrNull(form.portionText) ?: return
        val mealDate = form.mealTime.toLocalDate()
        job = scope.launch {
            delay(DEBOUNCE_MS)
            onResult(checkPlanFit(patientUserId, food, portion, mealDate, LocalDate.now(clock)).getOrNull())
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}
