package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.PastConsultation
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestion
import java.time.Instant

/** My visits (with the sent reply). Nothing is stored on the phone: offline there is no data. */
data class MyConsultationsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val hasLoaded: Boolean = false,
    val next: NextFollowUp? = null,
    val checkInCard: CheckInCard = CheckInCard.Hidden,
    /** "Prepare for your visit": empty = the card does not appear (off, no data yet or error). */
    val suggestedQuestions: List<SuggestedQuestion> = emptyList(),
    val past: List<PastConsultation> = emptyList(),
)

/** Check-in card: hidden (no visit, or its time passed without a reply), "Reply" or "sent". */
sealed interface CheckInCard {
    data object Hidden : CheckInCard
    data class Answer(val followUpId: Long) : CheckInCard

    /** @param canEdit "Edit my reply" only before the visit's time. */
    data class Sent(
        val followUpId: Long,
        val submittedAt: Instant,
        val editedAt: Instant?,
        val canEdit: Boolean,
    ) : CheckInCard
}
