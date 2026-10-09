package pe.edu.upc.healthify.features.monitoring.presentation.state

import pe.edu.upc.healthify.features.monitoring.domain.entity.NextFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.PastConsultation
import pe.edu.upc.healthify.features.monitoring.domain.entity.SuggestedQuestion
import java.time.Instant

/** PT25 · Mis consultas (+ PT25.3 con la respuesta enviada). Nada se guarda en el teléfono: sin conexión no hay datos. */
data class MyConsultationsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val loadFailed: Boolean = false,
    val hasLoaded: Boolean = false,
    val next: NextFollowUp? = null,
    val checkInCard: CheckInCard = CheckInCard.Hidden,
    /** «Prepara tu consulta» (IA-4): vacío = la card no aparece (apagada, aún sin datos o error). */
    val suggestedQuestions: List<SuggestedQuestion> = emptyList(),
    val past: List<PastConsultation> = emptyList(),
)

/** Card del check-in: oculta (sin consulta o ya pasó su hora sin respuesta), «Responder» o PT25.3 «enviada». */
sealed interface CheckInCard {
    data object Hidden : CheckInCard
    data class Answer(val followUpId: Long) : CheckInCard

    /** @param canEdit «Editar mi respuesta» solo antes de la hora de la consulta (MA-4). */
    data class Sent(
        val followUpId: Long,
        val submittedAt: Instant,
        val editedAt: Instant?,
        val canEdit: Boolean,
    ) : CheckInCard
}
