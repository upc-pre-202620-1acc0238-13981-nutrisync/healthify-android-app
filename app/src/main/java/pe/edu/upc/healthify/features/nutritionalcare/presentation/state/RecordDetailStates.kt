package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EvaluationDetail
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordReferral
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReferralSummary
import java.time.Instant

/** Sheet with the detail of an assessment tapped in "ASSESSMENTS". */
sealed interface EvaluationSheetState {
    data object Loading : EvaluationSheetState
    data object Failed : EvaluationSheetState
    data class Loaded(val detail: EvaluationDetail) : EvaluationSheetState
}

/** A referral ready for its detail sheet (patient's and nutritionist's record). */
data class ReferralDetailItem(
    val specialty: String,
    val issuedAt: Instant,
    val isOpen: Boolean,
    val reason: String = "",
    val closedAt: Instant? = null,
)

fun ReferralSummary.toDetailItem() = ReferralDetailItem(specialty, issuedAt, isOpen, reason, closedAt)

fun RecordReferral.toDetailItem() = ReferralDetailItem(specialty, issuedAt, isOpen, reason, closedAt)
