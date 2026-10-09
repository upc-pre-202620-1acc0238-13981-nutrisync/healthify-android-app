package pe.edu.upc.healthify.testing

import androidx.lifecycle.SavedStateHandle
import pe.edu.upc.healthify.features.carerelationship.domain.entity.IssuedInvitation
import pe.edu.upc.healthify.features.carerelationship.domain.entity.PatientRoster
import pe.edu.upc.healthify.features.carerelationship.domain.entity.RosterPatient
import pe.edu.upc.healthify.features.carerelationship.domain.repository.PractitionerCareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ClinicalReason
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.monitoring.domain.entity.MonitoringSummary
import pe.edu.upc.healthify.features.monitoring.domain.entity.PatientMonitoringPanel
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerMonitoringRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.CalculationBasis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ComplianceRatio
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationDiagnosis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationTargets
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.GuidelineSuggestions
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PublicationDraft
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SinceLastConsultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetInputs
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetParameters
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ConsultationRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.NutritionPlanRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientBaselineRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientSummaryRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PractitionerRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import java.time.Instant
import java.time.LocalDate
import pe.edu.upc.healthify.features.monitoring.domain.valueobject.PatientId as MonitoringPatientId

const val PATIENT_ID = 7L

/** `SavedStateHandle` con los argumentos de las rutas del nutricionista (por nombre de propiedad). */
fun patientArgs(editing: Boolean? = null, careLinkId: Long? = 4) = SavedStateHandle(
    buildMap {
        put("patientId", PATIENT_ID)
        put("patientName", "Ana Flores")
        careLinkId?.let { put("careLinkId", it) }
        editing?.let { put("editing", it) }
    },
)

fun rosterPatient(id: Long, name: String, isNew: Boolean = false, hasBaseline: Boolean = !isNew) = RosterPatient(
    patientId = id,
    careLinkId = CareLinkId(id + 100),
    fullName = name,
    linkedSince = Instant.parse("2026-03-12T15:00:00Z"),
    isLinkActive = true,
    hasBaseline = hasBaseline,
    activePlanVersion = if (isNew) null else 3,
    isNew = isNew,
    hasConsultationInProgress = false,
)

class FakePractitionerCareLinkRepository : PractitionerCareLinkRepository {
    var rosterResult: Result<PatientRoster> = Result.success(PatientRoster(emptyList()))
    var invitationResult: Result<IssuedInvitation> = Result.success(invitation())
    val issuedExpirations = mutableListOf<Instant>()
    var rosterCalls = 0

    override suspend fun getRoster(practitionerId: Long): Result<PatientRoster> {
        rosterCalls++
        return rosterResult
    }

    override suspend fun issueInvitation(expiresAt: Instant): Result<IssuedInvitation> {
        issuedExpirations += expiresAt
        return invitationResult
    }

    var dischargeResult: Result<Unit> = Result.success(Unit)
    val discharges = mutableListOf<Pair<Long, String>>()

    override suspend fun discharge(careLinkId: CareLinkId, reason: ClinicalReason): Result<Unit> {
        discharges += careLinkId.value to reason.text
        return dischargeResult
    }
}

fun invitation(id: Long = 5, expiresAt: Instant = CONSULTATIONS_NOW.plusSeconds(86_400)) =
    IssuedInvitation(id, InvitationToken("Q2hhbmdlTWVQbGVhc2VUb2tlbjEyMzQ1Njc4OTA"), expiresAt)

fun patientBaseline() = PatientBaseline(
    birthDate = LocalDate.of(1995, 2, 15),
    ageYears = 31,
    sex = BiologicalSex.FEMALE,
    heightCm = 168.0,
    conditions = setOf(MedicalCondition.HYPOTHYROIDISM),
)

class FakePatientBaselineRepository : PatientBaselineRepository {
    var getResult: Result<PatientBaseline?> = Result.success(patientBaseline())
    var recordResult: Result<PatientBaseline> = Result.success(patientBaseline())
    var updateResult: Result<PatientBaseline> = Result.success(patientBaseline())
    val recorded = mutableListOf<NewBaseline>()
    val updated = mutableListOf<NewBaseline>()

    override suspend fun get(patientId: PatientId): Result<PatientBaseline?> = getResult

    override suspend fun record(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline> {
        recorded += baseline
        return recordResult
    }

    override suspend fun update(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline> {
        updated += baseline
        return updateResult
    }
}

fun patientSummary(
    baseline: BaselineSummary? = BaselineSummary(BiologicalSex.FEMALE, 31, 168.0, setOf(MedicalCondition.HYPOTHYROIDISM)),
    activePlanVersion: Int? = 3,
    inProgress: pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationInProgress? = null,
    nextFollowUp: pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SummaryFollowUp? = null,
) = PatientSummary(
    patientId = PATIENT_ID,
    fullName = "Ana Flores",
    linkedSince = Instant.parse("2026-03-12T15:00:00Z"),
    isLinkActive = true,
    activePlanVersion = activePlanVersion,
    baseline = baseline,
    nextFollowUp = nextFollowUp,
    consultationInProgress = inProgress,
    sinceLastConsultation = SinceLastConsultation(LocalDate.parse("2026-09-30"), -0.3, 4, ComplianceRatio(5, 7)),
)

class FakePatientSummaryRepository : PatientSummaryRepository {
    var result: Result<PatientSummary> = Result.success(patientSummary())
    var calls = 0

    override suspend fun getSummary(patientId: PatientId): Result<PatientSummary> {
        calls++
        return result
    }
}

class FakePractitionerRecordRepository : PractitionerRecordRepository {
    var result: Result<PractitionerPatientRecord> =
        Result.success(PractitionerPatientRecord(null, null, null, null, null, emptyList(), emptyList()))

    override suspend fun getRecord(patientId: PatientId): Result<PractitionerPatientRecord> = result
}

class FakeNutritionPlanRepository : NutritionPlanRepository {
    var result: Result<PlanHistory> = Result.success(PlanHistory(emptyList()))

    override suspend fun getHistory(patientId: PatientId): Result<PlanHistory> = result
}

fun savedMeasurement() = ConsultationMeasurement(
    weightKg = 74.2,
    heightCm = 168.0,
    waistCm = 88.0,
    bodyFatPercentage = null,
    bmi = 26.3,
    protocolChecks = setOf(ProtocolCheck.FASTING, ProtocolCheck.NO_SHOES),
    activityLevel = ActivityLevel.MODERATE,
    habits = null,
    biochemistry = null,
    ageYears = 31,
    sex = BiologicalSex.FEMALE,
)

val sampleTargets = Targets(1796.0, 119.0, 195.0, 60.0)
val sampleBasis = CalculationBasis(EnergyEquation.MIFFLIN_ST_JEOR, 74.2, 1.55, DeficitKind.FIXED_KCAL, 500.0)

fun consultation(
    step: ConsultationStep = ConsultationStep.MEASUREMENT,
    measurement: ConsultationMeasurement? = null,
    diagnosis: ConsultationDiagnosis? = null,
    targets: ConsultationTargets? = null,
    draft: PublicationDraft? = null,
    checkIn: PatientCheckIn? = null,
    isInProgress: Boolean = true,
) = Consultation(
    id = ConsultationId(9),
    patientId = PATIENT_ID,
    isInProgress = isInProgress,
    currentStep = step,
    lastSavedAt = CONSULTATIONS_NOW,
    measurement = measurement,
    diagnosis = diagnosis,
    targets = targets,
    publicationDraft = draft,
    patientCheckIn = checkIn,
    publishedPlanVersion = null,
)

/** Como lo devuelve el backend en una consulta en curso: pendiente hasta publicar (NC-7). */
fun issuedDiagnosis(
    code: DiagnosisCode = DiagnosisCode.OVERWEIGHT_GRADE_I,
    source: DiagnosisSource = DiagnosisSource.PRACTITIONER_SELECTED,
    pending: Boolean = true,
) = ConsultationDiagnosis(code, source, "IMC 26.3", pending)

fun prescribedTargets(overridden: Boolean = false) =
    ConsultationTargets(sampleBasis, sampleTargets, sampleTargets, overridden, if (overridden) "Su ritmo" else null)

/** Respuestas programables por paso; registra lo que se envió. */
class FakeConsultationRepository : ConsultationRepository {
    var startResult: Result<Consultation> = Result.success(consultation())
    var inProgressResult: Result<Consultation?> = Result.success(consultation())
    var measurementResult: Result<Consultation>? = null
    var suggestionResult: Result<DiagnosisSuggestion> = Result.success(
        DiagnosisSuggestion(77, DiagnosisCode.OVERWEIGHT_GRADE_I, "IMC 26.3 kg/m²", isFromAi = true, disclaimer = "Revísala."),
    )
    var diagnosisResult: Result<Consultation>? = null
    var proposalResult: Result<TargetProposal> = Result.success(
        TargetProposal(sampleBasis, sampleTargets, TargetInputs(BiologicalSex.FEMALE, 31, 168.0, 74.2, ActivityLevel.MODERATE)),
    )
    var prescribeResult: Result<Consultation>? = null
    var guidelinesResult: Result<GuidelineSuggestions> = Result.success(GuidelineSuggestions(emptyList(), isFromAi = false))
    var draftResult: Result<Consultation> = Result.success(consultation())

    /** Una respuesta por intento de publicar; la última se repite. */
    var publishResults: List<Result<Consultation>> = listOf(Result.success(consultation(isInProgress = false)))

    val starts = mutableListOf<Long?>()
    val measurements = mutableListOf<NewMeasurement>()
    val diagnoses = mutableListOf<DiagnosisChoice>()
    val proposals = mutableListOf<TargetParameters?>()
    val prescriptions = mutableListOf<TargetPrescription>()
    val drafts = mutableListOf<Publication>()
    val publications = mutableListOf<Pair<Publication, IdempotencyKey>>()
    var inProgressCalls = 0

    override suspend fun start(patientId: PatientId, scheduledFollowUpId: Long?): Result<Consultation> {
        starts += scheduledFollowUpId
        return startResult
    }

    override suspend fun getInProgress(patientId: PatientId): Result<Consultation?> {
        inProgressCalls++
        return inProgressResult
    }

    override suspend fun recordMeasurement(id: ConsultationId, measurement: NewMeasurement): Result<Consultation> {
        measurements += measurement
        return measurementResult ?: Result.success(consultation(ConsultationStep.DIAGNOSIS, measurement = savedMeasurement()))
    }

    override suspend fun suggestDiagnosis(id: ConsultationId): Result<DiagnosisSuggestion> = suggestionResult

    override suspend fun issueDiagnosis(id: ConsultationId, choice: DiagnosisChoice): Result<Consultation> {
        diagnoses += choice
        return diagnosisResult ?: Result.success(
            consultation(ConsultationStep.TARGETS, savedMeasurement(), issuedDiagnosis(choice.code, choice.source)),
        )
    }

    override suspend fun proposeTargets(id: ConsultationId, parameters: TargetParameters?): Result<TargetProposal> {
        proposals += parameters
        return proposalResult
    }

    override suspend fun prescribeTargets(id: ConsultationId, prescription: TargetPrescription): Result<Consultation> {
        prescriptions += prescription
        return prescribeResult ?: Result.success(
            consultation(ConsultationStep.PUBLICATION, savedMeasurement(), issuedDiagnosis(), prescribedTargets()),
        )
    }

    override suspend fun suggestGuidelines(id: ConsultationId): Result<GuidelineSuggestions> = guidelinesResult

    override suspend fun saveDraft(id: ConsultationId, publication: Publication): Result<Consultation> {
        drafts += publication
        return draftResult
    }

    override suspend fun publish(
        id: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation> {
        val result = publishResults[minOf(publications.size, publishResults.lastIndex)]
        publications += publication to idempotencyKey
        return result
    }
}

class FakePractitionerMonitoringRepository : PractitionerMonitoringRepository {
    var panelResult: Result<PatientMonitoringPanel> =
        Result.success(PatientMonitoringPanel(LocalDate.parse("2026-10-07"), null, null, null, emptyList(), null))
    var summaryResult: Result<MonitoringSummary> = Result.success(MonitoringSummary("Cumple sus metas casi todos los días."))
    val panelDates = mutableListOf<LocalDate>()

    override suspend fun getPanel(patientId: MonitoringPatientId, date: LocalDate, days: Int): Result<PatientMonitoringPanel> {
        panelDates += date
        return panelResult
    }

    override suspend fun getSummary(patientId: MonitoringPatientId): Result<MonitoringSummary> = summaryResult
}
