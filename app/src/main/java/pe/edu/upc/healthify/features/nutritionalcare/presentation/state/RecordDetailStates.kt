package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EvaluationDetail
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordReferral
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReferralSummary
import java.time.Instant

/** PAC-3 · hoja con el detalle de una evaluación tocada en «EVALUACIONES». */
sealed interface EvaluationSheetState {
    data object Loading : EvaluationSheetState
    data object Failed : EvaluationSheetState
    data class Loaded(val detail: EvaluationDetail) : EvaluationSheetState
}

/** Una derivación lista para su hoja de detalle (PT20 del paciente y PAC-3 del nutricionista). */
data class ReferralDetailItem(
    val specialty: String,
    val issuedAt: Instant,
    val isOpen: Boolean,
    val reason: String = "",
    val closedAt: Instant? = null,
)

fun ReferralSummary.toDetailItem() = ReferralDetailItem(specialty, issuedAt, isOpen, reason, closedAt)

fun RecordReferral.toDetailItem() = ReferralDetailItem(specialty, issuedAt, isOpen, reason, closedAt)
