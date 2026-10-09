package pe.edu.upc.healthify.testing

import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerAgendaRepository
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.ConsultationModality
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.AcceptedProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAdjustmentProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalStatus
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewEvidence
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ReviewInboxRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import java.time.Instant
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId as MonitoringPatientId

fun reviewItem(
    id: Long = 41,
    patientId: Long = PATIENT_ID,
    name: String? = "Ana Flores",
    signalType: SignalType = SignalType.SustainedDeviation,
    hasPlanProposal: Boolean = false,
    isOpen: Boolean = true,
) = ReviewItem(
    id = ReviewItemId(id),
    patientId = PatientId(patientId),
    patientFullName = name,
    signalType = signalType,
    legacyEvidence = "Logged 40% below target",
    evidence = ReviewEvidence.SustainedDeviation(40.0, 5, 9, DeviationDirection.BELOW, 718.0),
    isOpen = isOpen,
    resolvedWithAdjustment = null,
    resolutionNote = null,
    createdAt = Instant.parse("2026-09-08T15:00:00Z"),
    resolvedAt = null,
    hasPlanProposal = hasPlanProposal,
)

fun planProposal(
    reviewItemId: Long = 41,
    added: List<PlanGuidelineCode> = listOf(PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER),
    removed: List<PlanGuidelineCode> = emptyList(),
    status: PlanProposalStatus = PlanProposalStatus.PROPOSED,
) = PlanAdjustmentProposal(
    reviewItemId = ReviewItemId(reviewItemId),
    title = "Ajustar la energía y reforzar las cenas",
    currentEnergyKcal = 1796.0,
    proposed = Targets(1650.0, 119.0, 170.0, 55.0),
    addedGuidelines = added,
    removedGuidelines = removed,
    patientMessage = "Notamos que tus cenas son más ligeras.",
    recheckAfterDays = 7,
    rationale = "Registra menos energía en la cena.",
    generatedAt = Instant.parse("2026-09-08T16:00:00Z"),
    status = status,
)

/** Respuestas programables de la bandeja; `proposalResults` se consume en orden (la última se repite). */
class FakeReviewInboxRepository : ReviewInboxRepository {
    var itemsResult: Result<List<ReviewItem>> = Result.success(listOf(reviewItem()))
    var resolveResult: Result<ReviewItem>? = null
    var proposalResults: List<Result<PlanProposalLookup>> = listOf(Result.success(PlanProposalLookup.None))
    var acceptResult: Result<AcceptedProposal>? = null

    val requestedStates = mutableListOf<ReviewItemState>()
    val resolutions = mutableListOf<ReviewResolution>()
    val acceptances = mutableListOf<PlanAcceptance>()
    var proposalCalls = 0

    override suspend fun getItems(state: ReviewItemState): Result<List<ReviewItem>> {
        requestedStates += state
        return itemsResult
    }

    override suspend fun resolve(id: ReviewItemId, resolution: ReviewResolution): Result<ReviewItem> {
        resolutions += resolution
        return resolveResult ?: Result.success(reviewItem(id = id.value, isOpen = false))
    }

    override suspend fun getPlanProposal(id: ReviewItemId): Result<PlanProposalLookup> {
        val result = proposalResults[minOf(proposalCalls, proposalResults.lastIndex)]
        proposalCalls++
        return result
    }

    override suspend fun acceptPlanProposal(id: ReviewItemId, acceptance: PlanAcceptance): Result<AcceptedProposal> {
        acceptances += acceptance
        return acceptResult ?: Result.success(AcceptedProposal(reviewItem(id = id.value, isOpen = false), planVersion = 4))
    }
}

fun agendaVisit(id: Long = 31, patientId: Long = PATIENT_ID, scheduledFor: Instant = Instant.parse("2026-09-18T15:00:00Z")) =
    AgendaVisit(
        id = id,
        patientId = MonitoringPatientId(patientId),
        patientFullName = "Ana Flores",
        scheduledFor = scheduledFor,
        state = FollowUpState.SCHEDULED,
        preparation = emptyList(),
        modality = ConsultationModality.IN_PERSON,
    )

class FakePractitionerAgendaRepository : PractitionerAgendaRepository {
    var agendaResult: Result<List<AgendaVisit>> = Result.success(emptyList())
    var scheduleResult: Result<AgendaVisit>? = null
    var rescheduleResult: Result<AgendaVisit>? = null
    var cancelResult: Result<Unit> = Result.success(Unit)
    var referralResult: Result<Unit> = Result.success(Unit)

    val scheduled = mutableListOf<NewFollowUp>()
    val rescheduled = mutableListOf<Triple<Long, Instant, FollowUpPreparation>>()
    val cancelled = mutableListOf<Long>()
    val referrals = mutableListOf<NewReferral>()
    val agendaRequests = mutableListOf<Pair<FollowUpState?, Instant?>>()

    override suspend fun getAgenda(practitionerId: Long, state: FollowUpState?, from: Instant?): Result<List<AgendaVisit>> {
        agendaRequests += state to from
        return agendaResult
    }

    override suspend fun schedule(followUp: NewFollowUp): Result<AgendaVisit> {
        scheduled += followUp
        return scheduleResult ?: Result.success(agendaVisit(scheduledFor = followUp.scheduledFor))
    }

    override suspend fun cancel(followUpId: Long, reason: CancellationReason?): Result<Unit> {
        cancelled += followUpId
        return cancelResult
    }

    override suspend fun reschedule(followUpId: Long, scheduledFor: Instant, preparation: FollowUpPreparation): Result<AgendaVisit> {
        rescheduled += Triple(followUpId, scheduledFor, preparation)
        return rescheduleResult ?: Result.success(agendaVisit(id = followUpId, scheduledFor = scheduledFor))
    }

    override suspend fun recordReferral(referral: NewReferral): Result<Unit> {
        referrals += referral
        return referralResult
    }
}
