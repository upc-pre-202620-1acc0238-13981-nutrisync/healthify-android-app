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

/** Una fila de PR13: «Ana Flores · desviación sostenida» y «Recibido el 8 sept. 2026». */
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
 * PR13 · Bandeja de revisión (+ PR13.V al día y PR13.1 con el Snackbar, que muestra el shell). `items = null` =
 * todavía no se leyó. La bandeja no se guarda en el teléfono: sin conexión y sin lectura previa → estado offline.
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

/** Encabezado de PR14: chip «Desviación sostenida · recibido el 8 sept.» y la evidencia armada en la app. */
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

/** La propuesta de IA de PR14.IA. */
sealed interface ProposalSection {
    /** El ítem no ofrece propuesta (otra señal, o `404`): PR14 normal. */
    data object NotOffered : ProposalSection

    /** `202`: la IA la está preparando (se reintenta solo). */
    data object Generating : ProposalSection

    data class Ready(val proposal: PlanAdjustmentProposal) : ProposalSection
}

/** Por qué no se pudo abrir el ítem. */
enum class ReviewLoadError { NOT_AVAILABLE, OFFLINE, GENERIC }

/** Avisos de PR14 / PR14.IA / PR14.IA-A (textos de las Notas). */
enum class ReviewDialog {
    /** `404 ReviewItemNotFound`: «Este ítem ya no está disponible.» */
    NOT_AVAILABLE,

    /** `409 PlanProposalAlreadyDecided`: «Este ítem ya fue resuelto.» */
    ALREADY_RESOLVED,

    /** `403 ActiveCareLinkRequired` o sin vínculo en la cartera. */
    NO_ACTIVE_LINK,

    /** `409 PlanVersionAlreadySuperseded`: el plan vigente cambió mientras se revisaba. */
    PLAN_CHANGED,

    /** Sin conexión, 5xx o respuesta inesperada: «Reintentar». */
    SERVER_ERROR,
}

/**
 * PR14 · Resolver ítem (+ PR14.1 «Responde Sí o No») y PR14.IA · plan propuesto por IA. «Resolver» de PR14 envía la
 * resolución; «Resolver» de PR14.IA asigna el plan tal cual. «Resolver sin asignar este plan» muestra el formulario de
 * PR14 (la propuesta queda descartada y el plan no cambia).
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
    /** El formulario Sí/No + nota de PR14 (sin propuesta, o al elegir resolver sin asignarla). */
    val showsResolutionForm: Boolean
        get() = header != null && (proposal == ProposalSection.NotOffered || showManualResolution)

    val isBusy: Boolean get() = isResolving || isAccepting || isOpeningPatient
}

/** PR14.IA-A · Ajustar el plan propuesto: todo viene prellenado con la propuesta. */
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
    /** `422 PlanProposalOutOfSafetyBounds`: la energía queda bajo el piso calórico del paciente. */
    val belowSafetyFloor: Boolean = false,
    val isAssigning: Boolean = false,
    val isOffline: Boolean = false,
    val dialog: ReviewDialog? = null,
)
