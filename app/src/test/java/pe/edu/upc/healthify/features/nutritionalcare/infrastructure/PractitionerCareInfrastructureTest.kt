package pe.edu.upc.healthify.features.nutritionalcare.infrastructure

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanRestrictionItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.CustomGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.OverrideReason
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientMessage
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toPlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ConsultationService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PatientBaselineService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ConsultationDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisSuggestionDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.GuidelineSuggestionsDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.MeasurementRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.NutritionPlanDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientSummaryDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PractitionerRecordDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PrescribeTargetsRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PublicationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.StartConsultationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.ConsultationRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PatientBaselineRepositoryImpl
import retrofit2.HttpException
import retrofit2.Response

class PractitionerCareInfrastructureTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val consultationJson = """
        {
          "consultationId": 9, "patientId": 7, "practitionerId": 3, "state": "InProgress",
          "currentStep": "Publication", "stepNumber": 4,
          "startedAt": "2026-10-07T09:00:00-05:00", "lastSavedAt": "2026-10-07T09:40:00.1234567-05:00",
          "measurement": {
            "assessmentId": 21, "weightKg": 74.2, "heightCm": 168.0, "waistCm": 88, "bodyFatPercentage": null,
            "bmi": 26.3, "bmiCategory": "OverweightGradeI", "protocolChecks": ["Fasting", "NoShoes", "Mystery"],
            "activityLevel": "Moderate", "habits": { "mealsPerDay": 4, "waterLitersPerDay": null, "mealsOutPerWeek": null },
            "biochemistry": null, "ageYears": 31, "biologicalSex": "Female"
          },
          "diagnosis": {
            "diagnosisId": 5, "code": "OverweightGradeI", "source": "AiSuggestionAccepted",
            "rationale": "IMC 26.3", "bmiAtIssue": 26.3, "aiGenerationId": 77,
            "issuedAt": "2026-10-07T09:20:00-05:00", "isPending": true
          },
          "targets": {
            "planId": 40, "version": 4,
            "calculationBasis": { "equation": "MifflinStJeor", "referenceWeightKind": "Actual", "referenceWeightKg": 74.2,
              "activityFactor": 1.55, "deficitKind": "FixedKcal", "deficitValue": 500, "computedBmr": 1450, "computedTdee": 2247 },
            "proposal": { "energyKcal": 1796, "proteinG": 119, "carbG": 195, "fatG": 60 },
            "prescribed": { "energyKcal": 1700, "proteinG": 120, "carbG": 180, "fatG": 55 },
            "prescriptionOutcome": "Overridden", "overrideReason": "Su ritmo", "isPublished": false,
            "activePlanLegacyRestrictions": []
          },
          "publicationDraft": {
            "restrictions": ["ShellfishFree", "Unknown"], "guidelines": ["ReduceSalt"],
            "customGuidelines": ["Caminar 20 minutos"], "patientMessage": " "
          },
          "patientCheckIn": {
            "followUpId": 31, "feeling": "Fair", "difficulties": ["Dinners"],
            "questions": [{ "text": "¿Puedo comer fuera?", "origin": "Patient" }],
            "submittedAt": "2026-09-15T10:00:00-05:00", "editedAt": null, "isLocked": false
          },
          "completedAt": null, "publishedPlanVersion": null, "isFirstConsultation": false, "scheduledFollowUpId": null
        }
    """.trimIndent()

    @Test
    fun `consultation resource re-hydrates every saved step and drops unknown codes`() {
        val consultation = json.decodeFromString<ConsultationDto>(consultationJson).toDomain()

        assertEquals(ConsultationId(9), consultation.id)
        assertTrue(consultation.isInProgress)
        assertEquals(ConsultationStep.PUBLICATION, consultation.currentStep)
        val measurement = consultation.measurement!!
        assertEquals(setOf(ProtocolCheck.FASTING, ProtocolCheck.NO_SHOES), measurement.protocolChecks)
        assertEquals(ActivityLevel.MODERATE, measurement.activityLevel)
        assertEquals(4, measurement.habits?.mealsPerDay)
        assertEquals(DiagnosisSource.AI_SUGGESTION_ACCEPTED, consultation.diagnosis?.source)
        // NC-7: en una consulta en curso el diagnóstico del paso 2 llega pendiente hasta publicar.
        assertTrue(consultation.diagnosis!!.isPending)
        assertTrue(consultation.targets!!.isOverridden)
        assertEquals(Targets(1700.0, 120.0, 180.0, 55.0), consultation.targets?.prescribed)
        assertEquals(setOf(PlanRestrictionCode.SHELLFISH_FREE), consultation.publicationDraft?.restrictions)
        assertNull(consultation.publicationDraft?.patientMessage)
        assertEquals(listOf("¿Puedo comer fuera?"), consultation.patientCheckIn?.questions)
    }

    @Test
    fun `summary without baseline is PAC-0 and the step in progress is read by name`() {
        val summary = json.decodeFromString<PatientSummaryDto>(
            """
            { "patientId": 7, "fullName": "Luz Ramírez", "linkedSince": "2026-08-28T10:00:00-05:00",
              "linkStatus": "Active", "activePlanVersion": null, "baseline": null, "nextFollowUp": null,
              "consultationInProgress": { "consultationId": 9, "stepNumber": 2, "stepName": "Diagnosis",
                "lastSavedAt": "2026-10-07T09:40:00-05:00" },
              "sinceLastConsultation": { "fromDate": "2026-09-30", "weightSlopeKgPerWeek": null, "trendWeeks": 4,
                "compliance": { "met": 5, "total": 7 } } }
            """.trimIndent(),
        ).toDomain()

        assertFalse(summary.hasBaseline)
        assertTrue(summary.isNew)
        assertEquals(ConsultationStep.DIAGNOSIS, summary.consultationInProgress?.step)
        assertEquals(5, summary.sinceLastConsultation.compliance?.met)
        assertNull(summary.sinceLastConsultation.weightSlopeKgPerWeek)
    }

    @Test
    fun `practitioner record keeps the active diagnosis and sorts evaluations newest first`() {
        val record = json.decodeFromString<PractitionerRecordDto>(
            """
            { "patientId": 7, "identity": null, "care": null, "assessment": { "clinicalAnthropometrySeries": [] },
              "intervention": { "planVersion": 3, "validFrom": "2026-09-04T10:00:00-05:00", "energyKcal": 1796,
                "proteinG": 119, "carbG": 195, "fatG": 60, "guidelines": [], "restrictions": [] },
              "followUp": { "dailyCompliance": [], "selfWeighInTrend": [], "consistency": null,
                "referrals": [{ "referralId": 2, "specialty": "Endocrinología", "reason": "Tiroides", "issuedBy": 3,
                  "issuedAt": "2026-09-08T10:00:00-05:00", "status": "Open" }] },
              "activeDiagnosis": { "diagnosisId": 5, "code": "OverweightGradeI", "statement": "", "issuedAt": "2026-03-03T10:00:00-05:00" },
              "evaluations": [
                { "assessmentId": 1, "takenAt": "2026-03-03T10:00:00-05:00", "weightKg": 76, "bmi": 26.9, "bmiCategory": "OverweightGradeI", "waistCm": null, "isFirst": true },
                { "assessmentId": 2, "takenAt": "2026-09-03T10:00:00-05:00", "weightKg": 74.2, "bmi": 26.3, "bmiCategory": "OverweightGradeI", "waistCm": 88, "isFirst": false }
              ],
              "clinicalWeight": { "latestKg": 74.2, "latestDate": "2026-09-03T10:00:00-05:00", "deltaKgSinceFirst": -1.8, "firstDate": "2026-03-03T10:00:00-05:00" },
              "bmi": { "value": 26.3, "category": "OverweightGradeI" },
              "compliance": { "from": "2026-09-30", "to": "2026-10-06", "met": 5, "total": 7 } }
            """.trimIndent(),
        ).toDomain()

        assertEquals(DiagnosisCode.OVERWEIGHT_GRADE_I, record.activeDiagnosis?.code)
        assertEquals(listOf(2L, 1L), record.evaluations.map { it.assessmentId })
        assertEquals("Endocrinología", record.referrals.single().specialty)
        assertTrue(record.referrals.single().isOpen)
        assertEquals(-1.8, record.clinicalWeight!!.deltaKgSinceFirst, 0.0)
        assertEquals(3, record.activeTargets?.planVersion)
    }

    @Test
    fun `plan history skips unpublished drafts and keeps custom and legacy texts as written`() {
        val history = json.decodeFromString<List<NutritionPlanDto>>(
            """
            [
              { "planId": 1, "version": 1, "proposal": { "energyKcal": 1900, "proteinG": 110, "carbG": 220, "fatG": 63 },
                "isActive": false, "publishedAt": "2026-03-12T10:00:00-05:00", "guidelines": ["Comer despacio"],
                "restrictions": [], "legacyRestrictions": ["Sin ají"] },
              { "planId": 2, "version": 2, "proposal": { "energyKcal": 1850, "proteinG": 110, "carbG": 210, "fatG": 62 },
                "prescribed": { "energyKcal": 1796, "proteinG": 119, "carbG": 195, "fatG": 60 },
                "isActive": true, "publishedAt": "2026-09-04T10:00:00-05:00",
                "guidelineItems": [{ "code": "ReduceSalt" }, { "custom": "Caminar 20 minutos" }],
                "restrictions": ["ShellfishFree"] },
              { "planId": 3, "version": 3, "proposal": { "energyKcal": 1700, "proteinG": 120, "carbG": 180, "fatG": 55 },
                "isActive": false, "publishedAt": null }
            ]
            """.trimIndent(),
        ).toPlanHistory()

        assertEquals(listOf(2, 1), history.versions.map { it.version })
        val active = history.active!!
        assertEquals(1796.0, active.targets.energyKcal, 0.0)
        assertEquals(
            listOf(PlanGuidelineItem.Catalog(PlanGuidelineCode.REDUCE_SALT), PlanGuidelineItem.Custom("Caminar 20 minutos")),
            active.guidelines,
        )
        assertEquals(listOf(PlanRestrictionItem.Catalog(PlanRestrictionCode.SHELLFISH_FREE)), active.restrictions)
        val first = history.versions.last()
        assertEquals(listOf(PlanGuidelineItem.Custom("Comer despacio")), first.guidelines)
        assertEquals(listOf(PlanRestrictionItem.Legacy("Sin ají")), first.restrictions)
    }

    @Test
    fun `requests carry the backend codes and omit what was not filled in`() {
        val measurement = json.encodeToString(
            MeasurementRequestDto.serializer(),
            pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewMeasurement(
                74.2, null, null, setOf(ProtocolCheck.NO_SHOES, ProtocolCheck.FASTING), ActivityLevel.LIGHT, null, null,
            ).toDto(),
        )
        assertEquals("""{"weightKg":74.2,"protocolChecks":["Fasting","NoShoes"],"activityLevel":"Light"}""", measurement)

        assertEquals(
            DiagnosisRequestDto("OverweightGradeI", "AiSuggestionAccepted", 77, "IMC"),
            DiagnosisChoice.AcceptAiSuggestion(DiagnosisCode.OVERWEIGHT_GRADE_I, 77, "IMC").toDto(),
        )
        assertEquals(
            DiagnosisRequestDto("ObesityGradeI", "PractitionerSelected"),
            DiagnosisChoice.Selected(DiagnosisCode.OBESITY_GRADE_I).toDto(),
        )
        assertEquals(
            PrescribeTargetsRequestDto("Overridden", 1700.0, 120.0, 180.0, 55.0, "Su ritmo"),
            TargetPrescription.Overridden(Targets(1700.0, 120.0, 180.0, 55.0), OverrideReason(" Su ritmo ")).toDto(),
        )
        assertEquals("{}", json.encodeToString(TargetProposalRequestDto.serializer(), TargetProposalRequestDto()))
        assertEquals("{}", json.encodeToString(StartConsultationRequestDto.serializer(), StartConsultationRequestDto()))

        val publication = Publication(
            restrictions = setOf(PlanRestrictionCode.HALAL, PlanRestrictionCode.LACTOSE_FREE),
            guidelines = setOf(PlanGuidelineCode.REDUCE_SALT),
            customGuidelines = listOf(CustomGuideline("Caminar 20 minutos")),
            patientMessage = PatientMessage("¡Vamos bien!"),
        ).toDto()
        assertEquals(
            PublicationRequestDto(listOf("LactoseFree", "Halal"), listOf("ReduceSalt"), listOf("Caminar 20 minutos"), "¡Vamos bien!"),
            publication,
        )
    }

    @Test
    fun `baseline 404 means no baseline yet and other errors stay errors`() = runTest {
        val service = FakeBaselineService()
        val repository = PatientBaselineRepositoryImpl(service)

        service.get = { throw httpError(404, "BaselineNotFound") }
        assertEquals(Result.success(null), repository.get(PatientId(7)))

        service.get = { throw httpError(403, "ActiveCareLinkRequired") }
        assertEquals(DomainError.Forbidden("ActiveCareLinkRequired"), repository.get(PatientId(7)).domainErrorOrNull())

        service.get = {
            PatientBaselineDto(7, "1995-02-15", 31, "Female", 168.0, listOf("Hypothyroidism", "Unknown"))
        }
        assertEquals(setOf(MedicalCondition.HYPOTHYROIDISM), repository.get(PatientId(7)).getOrNull()?.conditions)
    }

    @Test
    fun `no consultation in progress is null and publish sends the idempotency key`() = runTest {
        val service = FakeConsultationService(json.decodeFromString(consultationJson))
        val repository = ConsultationRepositoryImpl(service)

        service.inProgress = { throw httpError(404, "ConsultationNotFound") }
        assertEquals(Result.success(null), repository.getInProgress(PatientId(7)))

        val key = IdempotencyKey("abc-123")
        val publication = Publication(emptySet(), emptySet(), emptyList(), null)
        repository.publish(ConsultationId(9), publication, key)
        repository.publish(ConsultationId(9), publication, key)
        assertEquals(listOf("abc-123", "abc-123"), service.publishKeys)
    }

    @Test
    fun `a resource that breaks a domain rule is read as malformed instead of crashing`() = runTest {
        val broken = json.decodeFromString<ConsultationDto>(
            consultationJson.replace("\"energyKcal\": 1796", "\"energyKcal\": 0"),
        )
        val repository = ConsultationRepositoryImpl(FakeConsultationService(broken))

        val result = repository.getInProgress(PatientId(7))

        assertEquals(DomainError.Unexpected("MALFORMED_RESPONSE"), result.domainErrorOrNull())
    }

    private fun httpError(status: Int, code: String) =
        HttpException(Response.error<Any>(status, """{"code":"$code"}""".toResponseBody()))

    private class FakeBaselineService : PatientBaselineService {
        var get: () -> PatientBaselineDto = { error("not programmed") }

        override suspend fun get(patientId: Long): PatientBaselineDto = get.invoke()
        override suspend fun record(patientId: Long, body: PatientBaselineRequestDto): PatientBaselineDto = get.invoke()
        override suspend fun update(patientId: Long, body: PatientBaselineRequestDto): PatientBaselineDto = get.invoke()
    }

    private class FakeConsultationService(private val dto: ConsultationDto) : ConsultationService {
        var inProgress: () -> ConsultationDto = { dto }
        val publishKeys = mutableListOf<String>()

        override suspend fun start(patientId: Long, body: StartConsultationRequestDto) = dto
        override suspend fun getInProgress(patientId: Long) = inProgress()
        override suspend fun recordMeasurement(consultationId: Long, body: MeasurementRequestDto) = dto
        override suspend fun suggestDiagnosis(consultationId: Long) =
            DiagnosisSuggestionDto(code = "OverweightGradeI", source = "Rule")
        override suspend fun issueDiagnosis(consultationId: Long, body: DiagnosisRequestDto) = dto
        override suspend fun proposeTargets(consultationId: Long, body: TargetProposalRequestDto): TargetProposalDto =
            error("not used")
        override suspend fun prescribeTargets(consultationId: Long, body: PrescribeTargetsRequestDto) = dto
        override suspend fun suggestGuidelines(consultationId: Long) = GuidelineSuggestionsDto()
        override suspend fun saveDraft(consultationId: Long, body: PublicationRequestDto) = dto
        override suspend fun publish(consultationId: Long, idempotencyKey: String, body: PublicationRequestDto): ConsultationDto {
            publishKeys += idempotencyKey
            return dto
        }
    }
}
