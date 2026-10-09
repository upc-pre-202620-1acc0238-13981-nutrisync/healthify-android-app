package pe.edu.upc.healthify.features.nutritionalcare

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.R
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.designsystem.component.DayMonth
import pe.edu.upc.healthify.core.designsystem.component.DecimalNumber
import pe.edu.upc.healthify.core.designsystem.component.UiText
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.AwaitPlanProposalUseCase
import pe.edu.upc.healthify.features.nutritionalcare.application.usecase.GetOpenReviewItemUseCase
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanAcceptance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanProposalLookup
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditField
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEditValidation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ProposalEdits
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ResolutionNote
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewEvidence
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReviewResolution
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeviationDirection
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ReviewItemState
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SignalType
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ReviewItemService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.AcceptPlanProposalRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PlanProposalAcceptanceDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ResolveReviewItemRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ReviewItemDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.ReviewInboxRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.evidenceText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.labelText
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.mentionsLoggedDays
import pe.edu.upc.healthify.features.nutritionalcare.presentation.components.toUiText
import pe.edu.upc.healthify.testing.FakeReviewInboxRepository
import pe.edu.upc.healthify.testing.planProposal
import pe.edu.upc.healthify.testing.reviewItem
import retrofit2.Response
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewInboxTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private fun decodeItem(text: String) = json.decodeFromString(ReviewItemDto.serializer(), text).toDomain()

    // ----- Evidencia armada desde evidenceData (X-2) -----

    @Test
    fun `a sustained deviation is built from evidenceData, never from the english sentence`() {
        val item = decodeItem(
            """
            {"reviewItemId":41,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation",
             "evidence":"Logged on average 40% below the energy target on 5 of the last 9 logged days",
             "state":"Open","createdAt":"2026-09-08T10:00:00-05:00","patientFullName":"Ana Flores",
             "hasPlanProposal":true,
             "evidenceData":{"averagePercentFromTarget":-40.2,"deviatedDays":5,"loggedDaysConsidered":9,
                             "direction":"Below","averageEnergyKcalFromTarget":-718.4}}
            """.trimIndent(),
        )

        val evidence = item.evidence as ReviewEvidence.SustainedDeviation
        assertEquals(40.2, evidence.averagePercentFromTarget, 0.0)
        assertEquals(DeviationDirection.BELOW, evidence.direction)
        assertEquals(718.4, evidence.averageEnergyKcalFromTarget!!, 0.0)
        assertTrue(item.offersPlanProposal)
        assertTrue(item.mentionsLoggedDays)

        val text = item.evidenceText() as UiText.Resource
        assertEquals(R.string.review_evidence_deviation_named, text.id)
        assertEquals(
            listOf(
                "Ana Flores",
                40.0,
                UiText.of(R.string.review_direction_below),
                UiText.of(R.string.review_evidence_kcal_clause, 718.0),
                5,
                9,
            ),
            text.args,
        )
    }

    @Test
    fun `a deviation without the kcal (before X-2) or without a name uses the shorter templates`() {
        val evidence = ReviewEvidence.SustainedDeviation(25.0, 4, 7, DeviationDirection.ABOVE, averageEnergyKcalFromTarget = null)

        val text = evidence.toUiText(patientName = null) as UiText.Resource

        assertEquals(R.string.review_evidence_deviation, text.id)
        assertEquals(listOf(25.0, UiText.of(R.string.review_direction_above), UiText.Raw(""), 4, 7), text.args)
    }

    @Test
    fun `the direction comes from the sign when the backend omits it`() {
        val item = decodeItem(
            """{"reviewItemId":1,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation","state":"Open",
               "evidenceData":{"averagePercentFromTarget":-30,"deviatedDays":3,"loggedDaysConsidered":6}}""",
        )

        assertEquals(DeviationDirection.BELOW, (item.evidence as ReviewEvidence.SustainedDeviation).direction)
    }

    @Test
    fun `a scheduled recheck shows the adjustment day and version, with the counts only when there are`() {
        val withCounts = decodeItem(
            """{"reviewItemId":2,"patientId":7,"practitionerId":3,"signalType":"ScheduledRecheck","state":"Open",
               "evidenceData":{"adjustedOn":"2026-09-08","adjustedPlanVersion":3,"deviatedDays":2,"loggedDaysConsidered":6}}""",
        )
        val noCounts = decodeItem(
            """{"reviewItemId":3,"patientId":7,"practitionerId":3,"signalType":"ScheduledRecheck","state":"Open",
               "evidenceData":{"adjustedOn":"2026-09-08","adjustedPlanVersion":3}}""",
        )

        val counted = withCounts.evidenceText() as UiText.Resource
        assertEquals(R.string.review_evidence_recheck_counts, counted.id)
        assertEquals(listOf(DayMonth(LocalDate.of(2026, 9, 8)), 3, 2, 6), counted.args)
        assertTrue(withCounts.mentionsLoggedDays)

        val plain = noCounts.evidenceText() as UiText.Resource
        assertEquals(R.string.review_evidence_recheck, plain.id)
        assertFalse(noCounts.mentionsLoggedDays)
    }

    @Test
    fun `a consistency escalation joins only the clauses it has`() {
        val item = decodeItem(
            """{"reviewItemId":4,"patientId":7,"practitionerId":3,"signalType":"ConsistencyEscalation","state":"Open",
               "evidenceData":{"consistencyKgPerWeek":0.46,"consistencyState":"Alert","alertSinceOn":"2026-08-20",
                               "weeksInAlert":4,"shownToPatientOn":"2026-09-09"}}""",
        )

        val text = item.evidenceText() as UiText.Resource

        assertEquals(R.string.review_evidence_consistency, text.id)
        assertEquals(
            listOf(
                DecimalNumber(0.46, 2),
                UiText.of(R.string.review_alert_clause_weeks, DayMonth(LocalDate.of(2026, 8, 20)), 4),
                UiText.of(R.string.review_shown_clause, DayMonth(LocalDate.of(2026, 9, 9))),
            ),
            text.args,
        )
        assertEquals(UiText.of(R.string.signal_consistency_escalation_inline), item.signalType.labelText(inline = true))
    }

    @Test
    fun `missing or impossible evidenceData falls back to the legacy sentence as is`() {
        val legacy = decodeItem(
            """{"reviewItemId":5,"patientId":7,"practitionerId":3,"signalType":"ConsistencyEscalation",
               "evidence":"Consistency index 0.46 kg/week","state":"Open"}""",
        )
        val impossible = decodeItem(
            """{"reviewItemId":6,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation","evidence":"Legacy",
               "state":"Open","evidenceData":{"averagePercentFromTarget":-40,"deviatedDays":12,"loggedDaysConsidered":9,
               "direction":"Below"}}""",
        )
        val unknown = decodeItem(
            """{"reviewItemId":7,"patientId":7,"practitionerId":3,"signalType":"NewSignal","evidence":"Something","state":"Open"}""",
        )

        assertEquals(UiText.Raw("Consistency index 0.46 kg/week"), legacy.evidenceText())
        assertNull(impossible.evidence)
        assertEquals(UiText.Raw("Legacy"), impossible.evidenceText())
        assertEquals(SignalType.Custom("NewSignal"), unknown.signalType)
        assertEquals(UiText.Raw("NewSignal"), unknown.signalType.labelText())
    }

    @Test
    fun `resolution notes are read from their code, a custom note stays as written`() {
        val asIs = decodeItem(
            """{"reviewItemId":8,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation","state":"Resolved",
               "resolvedWithAdjustment":true,"resolutionNoteCode":"PlanAssignedAsIs","resolutionNoteData":{"planVersion":2}}""",
        )
        val custom = decodeItem(
            """{"reviewItemId":9,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation","state":"Resolved",
               "resolvedWithAdjustment":false,"resolutionNote":"Se conversó en consulta","resolutionNoteCode":"Custom"}""",
        )

        assertEquals(ResolutionNote.PlanAssignedAsIs(2), asIs.resolutionNote)
        assertFalse(asIs.isOpen)
        assertEquals(ResolutionNote.Custom("Se conversó en consulta"), custom.resolutionNote)
    }

    // ----- Reglas de dominio -----

    @Test
    fun `saying whether the plan was adjusted is required and an empty note is not sent`() {
        assertNull(ReviewResolution.of(null, "nota"))
        val resolution = ReviewResolution.of(true, "   ")!!
        assertTrue(resolution.resolvedWithAdjustment)
        assertNull(resolution.note)
        assertEquals(ResolveReviewItemRequestDto(false, "Hablado"), ReviewResolution.of(false, " Hablado ")!!.toDto())
    }

    @Test
    fun `the resulting guidelines are the current ones minus the removed plus the added`() {
        val proposal = planProposal(
            added = listOf(PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER),
            removed = listOf(PlanGuidelineCode.REDUCE_SALT),
        )

        val result = proposal.resultingGuidelines(listOf(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.REDUCE_SALT))

        assertEquals(setOf(PlanGuidelineCode.PRIORITIZE_VEGETABLES, PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER), result)
    }

    @Test
    fun `the edit form needs positive targets and a message of at most 500 characters`() {
        val invalid = ProposalEditValidation.of("", "0", "abc", "55", emptySet(), "x".repeat(PatientMessage.MAX_LENGTH + 1))
        assertEquals(
            setOf(ProposalEditField.ENERGY, ProposalEditField.PROTEIN, ProposalEditField.CARB, ProposalEditField.MESSAGE),
            (invalid as ProposalEditValidation.Invalid).fields,
        )

        val valid = ProposalEditValidation.of("1 650", "119", "170,5", "55", setOf(PlanGuidelineCode.REDUCE_SALT), "  ")
        val edits = (valid as ProposalEditValidation.Valid).edits
        assertEquals(Targets(1650.0, 119.0, 170.5, 55.0), edits.targets)
        assertNull(edits.patientMessage)
    }

    @Test
    fun `an acceptance with edits sends every target and the full guideline list in catalog order`() {
        val edits = ProposalEdits(
            targets = Targets(1700.0, 120.0, 180.0, 56.0),
            guidelines = setOf(PlanGuidelineCode.REDUCE_SALT, PlanGuidelineCode.PRIORITIZE_VEGETABLES),
            patientMessage = PatientMessage("Probemos estas ideas"),
        )

        assertEquals(AcceptPlanProposalRequestDto(asIs = true), PlanAcceptance.AsIs.toDto())
        assertEquals(
            AcceptPlanProposalRequestDto(
                asIs = false,
                energyKcal = 1700.0,
                proteinG = 120.0,
                carbG = 180.0,
                fatG = 56.0,
                guidelines = listOf("PrioritizeVegetables", "ReduceSalt"),
                patientMessage = "Probemos estas ideas",
            ),
            PlanAcceptance.WithEdits(edits).toDto(),
        )
    }

    // ----- Repositorio: 200 / 202 + Retry-After / 404 -----

    private class FakeReviewItemService : ReviewItemService {
        var proposalResponse: Response<JsonElement>? = null
        var items: List<ReviewItemDto> = emptyList()
        val states = mutableListOf<String>()

        override suspend fun getItems(state: String): List<ReviewItemDto> {
            states += state
            return items
        }

        override suspend fun resolve(reviewItemId: Long, body: ResolveReviewItemRequestDto): ReviewItemDto =
            error("not used")

        override suspend fun getPlanProposal(reviewItemId: Long): Response<JsonElement> = requireNotNull(proposalResponse)

        override suspend fun acceptPlanProposal(reviewItemId: Long, body: AcceptPlanProposalRequestDto): PlanProposalAcceptanceDto =
            error("not used")
    }

    private val service = FakeReviewItemService()
    private val repository = ReviewInboxRepositoryImpl(service, json)

    private fun accepted(retryAfter: String?): Response<JsonElement> {
        val raw = okhttp3.Response.Builder()
            .code(202)
            .message("Accepted")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/v1/review-items/41/plan-proposal").build())
            .apply { retryAfter?.let { header("Retry-After", it) } }
            .build()
        return Response.success(json.parseToJsonElement("""{"status":202,"title":"Generating"}"""), raw)
    }

    private fun problem(status: Int, code: String): Response<JsonElement> = Response.error(
        status,
        """{"status":$status,"title":"x","code":"$code"}""".toResponseBody("application/problem+json".toMediaType()),
    )

    private val proposalJson = """
        {"proposalId":9,"reviewItemId":41,"title":"Ajustar la energía","currentEnergyKcal":1796,"proposedEnergyKcal":1650,
         "proposedProteinG":119,"proposedCarbG":170,"proposedFatG":55,"addedGuidelines":["ProteinAndVegetablesAtDinner"],
         "removedGuidelines":[],"patientMessage":"Probemos","recheckAfterDays":7,"rationale":"Cenas ligeras",
         "generatedAt":"2026-09-08T16:00:00Z","status":"Proposed","assignedPlanVersion":null,
         "practitionerLanguage":"es","patientLanguage":"es"}
    """.trimIndent()

    @Test
    fun `202 is read as generating with its Retry-After, 200 as the proposal`() = runTest {
        service.proposalResponse = accepted("7")
        assertEquals(PlanProposalLookup.Generating(7), repository.getPlanProposal(ReviewItemId(41)).getOrThrow())

        service.proposalResponse = accepted(null)
        assertEquals(PlanProposalLookup.Generating(5), repository.getPlanProposal(ReviewItemId(41)).getOrThrow())

        service.proposalResponse = Response.success(json.parseToJsonElement(proposalJson))
        val ready = repository.getPlanProposal(ReviewItemId(41)).getOrThrow() as PlanProposalLookup.Ready
        assertEquals(1650.0, ready.proposal.proposed.energyKcal, 0.0)
        assertEquals(listOf(PlanGuidelineCode.PROTEIN_AND_VEGETABLES_AT_DINNER), ready.proposal.addedGuidelines)
        assertTrue(ready.proposal.canBeAccepted)
    }

    @Test
    fun `404 PlanProposalNotFound means PR14 without AI, another 404 is an error`() = runTest {
        service.proposalResponse = problem(404, "PlanProposalNotFound")
        assertEquals(PlanProposalLookup.None, repository.getPlanProposal(ReviewItemId(41)).getOrThrow())

        service.proposalResponse = problem(404, "ReviewItemNotFound")
        assertEquals(DomainError.NotFound("ReviewItemNotFound"), repository.getPlanProposal(ReviewItemId(41)).domainErrorOrNull())
    }

    @Test
    fun `a proposal that breaks an invariant is a malformed response`() = runTest {
        service.proposalResponse = Response.success(json.parseToJsonElement(proposalJson.replace("\"recheckAfterDays\":7", "\"recheckAfterDays\":0")))

        assertEquals(
            DomainError.Unexpected("MALFORMED_RESPONSE"),
            repository.getPlanProposal(ReviewItemId(41)).domainErrorOrNull(),
        )
    }

    @Test
    fun `the inbox asks for the open items and an item that is gone is null`() = runTest {
        service.items = listOf(
            json.decodeFromString(
                ReviewItemDto.serializer(),
                """{"reviewItemId":41,"patientId":7,"practitionerId":3,"signalType":"SustainedDeviation","state":"Open"}""",
            ),
        )
        val useCase = GetOpenReviewItemUseCase(repository)

        assertEquals(41L, useCase(41).getOrThrow()?.id?.value)
        assertNull(useCase(99).getOrThrow())
        assertEquals(listOf(ReviewItemState.OPEN.code, ReviewItemState.OPEN.code), service.states)
    }

    // ----- Use case: espera de la propuesta (202 → 200) -----

    @Test
    fun `awaiting the proposal retries after Retry-After until it is ready`() = runTest {
        val repo = FakeReviewInboxRepository().apply {
            proposalResults = listOf(
                Result.success(PlanProposalLookup.Generating(5)),
                Result.success(PlanProposalLookup.Generating(5)),
                Result.success(PlanProposalLookup.Ready(planProposal())),
            )
        }

        val emissions = AwaitPlanProposalUseCase(repo)(41).toList()

        assertEquals(3, emissions.size)
        assertEquals(PlanProposalLookup.Generating(5), emissions[0].getOrThrow())
        assertTrue(emissions.last().getOrThrow() is PlanProposalLookup.Ready)
        assertEquals(10_000L, currentTime)
        assertEquals(3, repo.proposalCalls)
    }

    @Test
    fun `if it is still generating after every attempt PR14 is shown without AI`() = runTest {
        val repo = FakeReviewInboxRepository().apply {
            proposalResults = listOf(Result.success(PlanProposalLookup.Generating(5)))
        }

        val emissions = AwaitPlanProposalUseCase(repo)(41, maxAttempts = 3).toList()

        assertEquals(PlanProposalLookup.None, emissions.last().getOrThrow())
        assertEquals(3, repo.proposalCalls)
    }

    @Test
    fun `a failure while waiting ends the wait with that failure`() = runTest {
        val repo = FakeReviewInboxRepository().apply {
            proposalResults = listOf(
                Result.success(PlanProposalLookup.Generating(5)),
                pe.edu.upc.healthify.testing.failureOf(DomainError.Network),
            )
        }

        val emissions = AwaitPlanProposalUseCase(repo)(41).toList()

        assertEquals(DomainError.Network, emissions.last().domainErrorOrNull())
        assertEquals(2, emissions.size)
    }

    @Test
    fun `only an open sustained deviation with a proposal opens PR14 IA`() {
        assertTrue(reviewItem(hasPlanProposal = true).offersPlanProposal)
        assertFalse(reviewItem(hasPlanProposal = true, signalType = SignalType.ConsistencyEscalation).offersPlanProposal)
        assertFalse(reviewItem(hasPlanProposal = true, isOpen = false).offersPlanProposal)
    }
}
