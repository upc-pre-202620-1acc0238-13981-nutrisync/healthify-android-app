package pe.edu.upc.healthify.features.nutritionalcare.presentation.state

import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAdjustmentProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.evidenceText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.mentionsLoggedDays
import java.time.Instant

/** An inbox row: "Ana Flores · sustained deviation" and "Received on Sep 8, 2026". */
data class InboxRow(
    val id: Long,
    val patientName: String?,
    val signal: UiText,
    val receivedAt: Instant?,
) {
    companion object {
        fun of(item: ReviewItem) = InboxRow(
            id = item.id.value,
            patientName = item.patientFullName,
            signal = item.signalType.labelText(inline = true),
            receivedAt = item.createdAt,
        )
    }
}

/**
 * Review inbox (plus the all-caught-up state and the "Signal resolved" snackbar, shown by the shell). `items = null`
 * = not read yet. The inbox is not stored on the phone: offline with no previous read → offline state.
 */
data class ReviewInboxUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val items: List<InboxRow>? = null,
) {
    val isEmpty: Boolean get() = items?.isEmpty() == true
    val needsConnection: Boolean get() = items == null && loadFailed && isOffline
}

/** Review item header: "Sustained deviation · received Sep 8" chip and the evidence built in the app. */
data class ReviewItemHeader(
    val patientName: String?,
    val signal: UiText,
    val receivedAt: Instant?,
    val evidence: UiText,
    val mentionsLoggedDays: Boolean,
) {
    companion object {
        fun of(item: ReviewItem) = ReviewItemHeader(
            patientName = item.patientFullName,
            signal = item.signalType.labelText(),
            receivedAt = item.createdAt,
            evidence = item.evidenceText(),
            mentionsLoggedDays = item.mentionsLoggedDays,
        )
    }
}

/** The AI proposal of a review item. */
sealed interface ProposalSection {
    /** The item offers no proposal (another signal, or `404`): the plain review item. */
    data object NotOffered : ProposalSection

    /** `202`: the AI is preparing it (retried automatically). */
    data object Generating : ProposalSection

    data class Ready(val proposal: PlanAdjustmentProposal) : ProposalSection
}

/** Why the item could not be opened. */
enum class ReviewLoadError { NOT_AVAILABLE, OFFLINE, GENERIC }

/** Notices of the review item, AI proposal and adjust screens (copy from the design notes). */
enum class ReviewDialog {
    /** `404 ReviewItemNotFound`: "This item is no longer available." */
    NOT_AVAILABLE,

    /** `409 PlanProposalAlreadyDecided`: "This item was already resolved." */
    ALREADY_RESOLVED,

    /** `403 ActiveCareLinkRequired` or no link in the caseload. */
    NO_ACTIVE_LINK,

    /** `409 PlanVersionAlreadySuperseded`: the current plan changed during the review. */
    PLAN_CHANGED,

    /** Offline, 5xx or unexpected response: "Retry". */
    SERVER_ERROR,
}

/**
 * Resolve item (plus "Answer Yes or No") and the AI-proposed plan. "Resolve" without a proposal sends the
 * resolution; "Resolve" with a proposal assigns the plan as is. "Resolve without assigning this plan" shows the
 * plain resolution form (the proposal is discarded and the plan does not change).
 */
data class ReviewItemUiState(
    val isLoading: Boolean = true,
    val loadError: ReviewLoadError? = null,
    val header: ReviewItemHeader? = null,
    val proposal: ProposalSection = ProposalSection.NotOffered,
    val showManualResolution: Boolean = false,
    val adjusted: Boolean? = null,
    val note: String = "",
    val showOutcomeError: Boolean = false,
    val isResolving: Boolean = false,
    val isAccepting: Boolean = false,
    val isOpeningPatient: Boolean = false,
    val isOffline: Boolean = false,
    val dialog: ReviewDialog? = null,
) {
    /** The Yes/No + note form (no proposal, or when choosing to resolve without assigning it). */
    val showsResolutionForm: Boolean
        get() = header != null && (proposal == ProposalSection.NotOffered || showManualResolution)

    val isBusy: Boolean get() = isResolving || isAccepting || isOpeningPatient
}

/** Adjust the proposed plan: everything comes prefilled from the proposal. */
data class AdjustProposalUiState(
    val isLoading: Boolean = true,
    val loadError: ReviewLoadError? = null,
    val patientFirstName: String? = null,
    val previousEnergyKcal: Double? = null,
    val energy: String = "",
    val protein: String = "",
    val carb: String = "",
    val fat: String = "",
    val guidelines: Set<PlanGuidelineCode> = emptySet(),
    val message: String = "",
    val invalidFields: Set<ProposalEditField> = emptySet(),
    /** `422 PlanProposalOutOfSafetyBounds`: the energy falls below the patient's calorie floor. */
    val belowSafetyFloor: Boolean = false,
    val isAssigning: Boolean = false,
    val isOffline: Boolean = false,
    val dialog: ReviewDialog? = null,
)
