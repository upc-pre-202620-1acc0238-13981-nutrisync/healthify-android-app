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
 * Asks for the "Was it in your plan?" suggestion when the dish, the portion or the day change,
 * with a short pause so it does not ask on every keystroke. Only the latest answer counts. Offline or with no
 * published plan there is no suggestion (`null`) and the patient answers as usual. One per ViewModel (not a singleton).
 */
class PlanFitSuggester @Inject constructor(
    private val checkPlanFit: CheckPlanFitUseCase,
    private val clock: Clock,
) {
    private var job: Job? = null

    /**
     * With no dish there is no suggestion ([onResult] with `null`); with a portion not valid yet (the patient is still
     * typing) the previous one is kept.
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
