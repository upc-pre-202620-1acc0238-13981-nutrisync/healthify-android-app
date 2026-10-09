package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper

import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BaselineSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.BiochemistryPanel
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.CalculationBasis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ComplianceRatio
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationDiagnosis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationInProgress
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ConsultationTargets
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EatingHabits
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.GuidelineSuggestions
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NutritionPlanVersion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientCheckIn
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanGuidelineItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanRestrictionItem
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PublicationDraft
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordActiveTargets
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordBmi
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ClinicalWeightSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordComplianceWindow
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordDiagnosis
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordEvaluation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.ReferralSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SinceLastConsultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.SummaryFollowUp
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetInputs
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetParameters
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Targets
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ActivityLevel
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.BiologicalSex
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationStep
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DeficitKind
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.DiagnosisSource
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.EnergyEquation
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.MedicalCondition
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanGuidelineCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PlanRestrictionCode
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ProtocolCheck
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.SuggestionSource
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.BiochemistryPanelDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.CalculationBasisDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.ConsultationDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.DiagnosisSuggestionDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.EatingHabitsDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.GuidelineSuggestionsDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.MeasurementRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.NutritionPlanDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientBaselineRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientSummaryDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PractitionerRecordDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PrescribeTargetsRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PublicationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetParametersDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetsDto
import java.time.Instant

/*
 * ACL de la consulta guiada y de las vistas del profesional. Un dato obligatorio ilegible (fecha, código de paso)
 * tumba la respuesta con `SerializationException` (→ `Unexpected("MALFORMED_RESPONSE")`); un código desconocido de
 * una lista cerrada se descarta.
 */

private fun malformed(what: String): Nothing = throw SerializationException("Unreadable $what")

private fun String.instantOrFail(what: String): Instant = toInstantOrNull() ?: malformed(what)

// ----- Datos base (NC-1) -----

fun PatientBaselineDto.toDomain(): PatientBaseline = PatientBaseline(
    birthDate = birthDate.toLocalDateOrNull() ?: malformed("birthDate"),
    ageYears = ageYears,
    sex = BiologicalSex.fromCode(biologicalSex) ?: malformed("biologicalSex"),
    heightCm = heightCm,
    conditions = conditions.mapNotNull(MedicalCondition::fromCode).toSet(),
)

fun NewBaseline.toDto(): PatientBaselineRequestDto = PatientBaselineRequestDto(
    birthDate = birthDate.value.toString(),
    biologicalSex = sex.code,
    heightCm = height.value,
    conditions = MedicalCondition.entries.filter { it in conditions }.map { it.code },
)

// ----- Consulta (NC-2 a NC-7) -----

fun ConsultationDto.toDomain(): Consultation = Consultation(
    id = ConsultationId(consultationId),
    patientId = patientId,
    isInProgress = state.equals("InProgress", ignoreCase = true),
    currentStep = ConsultationStep.fromCode(currentStep) ?: ConsultationStep.fromNumber(stepNumber)
        ?: malformed("currentStep"),
    lastSavedAt = lastSavedAt.instantOrFail("lastSavedAt"),
    measurement = measurement?.let { m ->
        ConsultationMeasurement(
            weightKg = m.weightKg,
            heightCm = m.heightCm,
            waistCm = m.waistCm,
            bodyFatPercentage = m.bodyFatPercentage,
            bmi = m.bmi,
            protocolChecks = m.protocolChecks.mapNotNull(ProtocolCheck::fromCode).toSet(),
            activityLevel = ActivityLevel.fromCode(m.activityLevel),
            habits = m.habits?.toDomain(),
            biochemistry = m.biochemistry?.toDomain(),
            ageYears = m.ageYears,
            sex = BiologicalSex.fromCode(m.biologicalSex),
        )
    },
    diagnosis = diagnosis?.let {
        ConsultationDiagnosis(
            code = DiagnosisCode.fromCode(it.code),
            source = DiagnosisSource.fromCode(it.source),
            rationale = it.rationale,
            isPending = it.isPending,
        )
    },
    targets = targets?.let {
        ConsultationTargets(
            basis = it.calculationBasis.toDomain(),
            proposal = it.proposal.toDomain(),
            prescribed = it.prescribed?.toDomain(),
            isOverridden = it.prescriptionOutcome.equals("Overridden", ignoreCase = true),
            overrideReason = it.overrideReason,
        )
    },
    publicationDraft = publicationDraft?.let { draft ->
        PublicationDraft(
            restrictions = draft.restrictions.mapNotNull(PlanRestrictionCode::fromCode).toSet(),
            guidelines = draft.guidelines.mapNotNull(PlanGuidelineCode::fromCode).toSet(),
            customGuidelines = draft.customGuidelines.filter { it.isNotBlank() },
            patientMessage = draft.patientMessage?.takeIf { it.isNotBlank() },
        )
    },
    patientCheckIn = patientCheckIn?.let { checkIn ->
        checkIn.submittedAt.toInstantOrNull()?.let { submittedAt ->
            PatientCheckIn(
                feeling = checkIn.feeling,
                difficulties = checkIn.difficulties,
                questions = checkIn.questions.map { it.text }.filter { it.isNotBlank() },
                submittedAt = submittedAt,
            )
        }
    },
    publishedPlanVersion = publishedPlanVersion,
)

private fun EatingHabitsDto.toDomain(): EatingHabits? =
    if (mealsPerDay == null && waterLitersPerDay == null && mealsOutPerWeek == null) {
        null
    } else {
        EatingHabits(mealsPerDay, waterLitersPerDay, mealsOutPerWeek)
    }

private fun BiochemistryPanelDto.toDomain(): BiochemistryPanel? =
    if (fastingGlucoseMgDl == null && totalCholesterolMgDl == null && triglyceridesMgDl == null) {
        null
    } else {
        BiochemistryPanel(fastingGlucoseMgDl, totalCholesterolMgDl, triglyceridesMgDl)
    }

private fun CalculationBasisDto.toDomain(): CalculationBasis = CalculationBasis(
    equation = EnergyEquation.fromCode(equation),
    referenceWeightKg = referenceWeightKg,
    activityFactor = activityFactor,
    deficitKind = DeficitKind.fromCode(deficitKind),
    deficitValue = deficitValue,
)

fun TargetsDto.toDomain(): Targets = Targets(energyKcal, proteinG, carbG, fatG)

fun NewMeasurement.toDto(): MeasurementRequestDto = MeasurementRequestDto(
    weightKg = weightKg,
    waistCm = waistCm,
    bodyFatPercentage = bodyFatPercentage,
    protocolChecks = ProtocolCheck.entries.filter { it in protocolChecks }.map { it.code },
    activityLevel = activityLevel.code,
    habits = habits?.let { EatingHabitsDto(it.mealsPerDay, it.waterLitersPerDay, it.mealsOutPerWeek) },
    biochemistry = biochemistry?.let {
        BiochemistryPanelDto(it.glucoseMgDl, it.totalCholesterolMgDl, it.triglyceridesMgDl)
    },
)

fun DiagnosisSuggestionDto.toDomain(): DiagnosisSuggestion = DiagnosisSuggestion(
    aiGenerationId = aiGenerationId,
    code = DiagnosisCode.fromCode(code) ?: malformed("diagnosis code"),
    rationale = rationale,
    isFromAi = SuggestionSource.fromCode(source) == SuggestionSource.AI && aiGenerationId != null,
    disclaimer = disclaimer,
)

fun DiagnosisChoice.toDto(): DiagnosisRequestDto = when (this) {
    is DiagnosisChoice.AcceptAiSuggestion -> DiagnosisRequestDto(
        code = code.code,
        source = source.code,
        aiGenerationId = aiGenerationId,
        rationale = rationale.takeIf { it.isNotBlank() },
    )
    is DiagnosisChoice.Selected -> DiagnosisRequestDto(
        code = code.code,
        source = source.code,
        rationale = rationale?.takeIf { it.isNotBlank() },
    )
}

fun TargetParameters.toDto(): TargetParametersDto = TargetParametersDto(
    equation = equation.code,
    deficitKind = deficitKind.code,
    deficitValue = deficitValue,
    proteinGramsPerKg = proteinGramsPerKg,
    fatPercentOfEnergy = fatPercentOfEnergy,
)

fun TargetProposalDto.toDomain(): TargetProposal = TargetProposal(
    basis = calculationBasis.toDomain(),
    proposal = proposal.toDomain(),
    inputs = TargetInputs(
        sex = BiologicalSex.fromCode(inputsSummary.sex),
        ageYears = inputsSummary.ageYears,
        heightCm = inputsSummary.heightCm,
        weightKg = inputsSummary.weightKg,
        activityLevel = ActivityLevel.fromCode(inputsSummary.activityLevel),
    ),
)

fun TargetPrescription.toDto(): PrescribeTargetsRequestDto = when (this) {
    TargetPrescription.AcceptedAsProposed -> PrescribeTargetsRequestDto(outcome = "AcceptedAsProposed")
    is TargetPrescription.Overridden -> PrescribeTargetsRequestDto(
        outcome = "Overridden",
        energyKcal = targets.energyKcal,
        proteinG = targets.proteinG,
        carbG = targets.carbG,
        fatG = targets.fatG,
        overrideReason = reason.value.trim(),
    )
}

fun GuidelineSuggestionsDto.toDomain(): GuidelineSuggestions = GuidelineSuggestions(
    codes = suggested.mapNotNull(PlanGuidelineCode::fromCode).distinct(),
    isFromAi = SuggestionSource.fromCode(source) == SuggestionSource.AI,
)

fun Publication.toDto(): PublicationRequestDto = PublicationRequestDto(
    restrictions = PlanRestrictionCode.entries.filter { it in restrictions }.map { it.code },
    guidelines = PlanGuidelineCode.entries.filter { it in guidelines }.map { it.code },
    customGuidelines = customGuidelines.map { it.text.trim() },
    patientMessage = patientMessage?.text?.trim(),
)

// ----- Vistas del profesional (RM-2, RM-4, PAC-4) -----

fun PatientSummaryDto.toDomain(): PatientSummary = PatientSummary(
    patientId = patientId,
    fullName = fullName?.takeIf { it.isNotBlank() },
    linkedSince = linkedSince?.toInstantOrNull(),
    isLinkActive = linkStatus.equals("Active", ignoreCase = true),
    activePlanVersion = activePlanVersion,
    baseline = baseline?.let {
        BaselineSummary(
            sex = BiologicalSex.fromCode(it.biologicalSex),
            ageYears = it.ageYears,
            heightCm = it.heightCm,
            conditions = it.conditions.mapNotNull(MedicalCondition::fromCode).toSet(),
        )
    },
    nextFollowUp = nextFollowUp?.let { next ->
        next.scheduledFor.toInstantOrNull()?.let { SummaryFollowUp(next.followUpId, it) }
    },
    consultationInProgress = consultationInProgress?.let { c ->
        ConsultationInProgress(
            id = ConsultationId(c.consultationId),
            step = ConsultationStep.fromCode(c.stepName) ?: ConsultationStep.fromNumber(c.stepNumber)
                ?: ConsultationStep.MEASUREMENT,
            lastSavedAt = c.lastSavedAt.instantOrFail("lastSavedAt"),
        )
    },
    sinceLastConsultation = SinceLastConsultation(
        fromDate = sinceLastConsultation.fromDate.toLocalDateOrNull() ?: malformed("fromDate"),
        weightSlopeKgPerWeek = sinceLastConsultation.weightSlopeKgPerWeek,
        trendWeeks = sinceLastConsultation.trendWeeks,
        compliance = sinceLastConsultation.compliance
            ?.takeIf { it.met in 0..it.total }
            ?.let { ComplianceRatio(it.met, it.total) },
    ),
)

fun PractitionerRecordDto.toDomain(): PractitionerPatientRecord = PractitionerPatientRecord(
    clinicalWeight = clinicalWeight?.let { w ->
        val latest = w.latestDate.toInstantOrNull()
        val first = w.firstDate.toInstantOrNull()
        if (latest != null && first != null) ClinicalWeightSummary(w.latestKg, latest, w.deltaKgSinceFirst, first) else null
    },
    bmi = bmi?.let { RecordBmi(it.value, DiagnosisCode.fromCode(it.category)) },
    activeTargets = intervention?.let { i ->
        i.validFrom.toInstantOrNull()?.let { RecordActiveTargets(i.planVersion, it, i.energyKcal) }
    },
    compliance = compliance?.let { c ->
        val from = c.from.toLocalDateOrNull()
        val to = c.to.toLocalDateOrNull()
        if (from != null && to != null && c.met in 0..c.total) {
            RecordComplianceWindow(from, to, ComplianceRatio(c.met, c.total))
        } else {
            null
        }
    },
    activeDiagnosis = activeDiagnosis?.let { d ->
        d.issuedAt.toInstantOrNull()?.let { RecordDiagnosis(DiagnosisCode.fromCode(d.code), d.statement, it) }
    },
    referrals = followUp?.referrals.orEmpty().mapNotNull { r ->
        r.issuedAt.toInstantOrNull()?.let {
            ReferralSummary(r.referralId, r.specialty, it, isOpen = r.status.equals("Open", ignoreCase = true))
        }
    },
    evaluations = evaluations.mapNotNull { e ->
        e.takenAt.toInstantOrNull()?.let { RecordEvaluation(e.assessmentId, it, e.weightKg, e.bmi, e.waistCm, e.isFirst) }
    }.sortedByDescending { it.takenAt },
)

/** Solo las versiones publicadas: un borrador de la consulta en curso no es historial. */
fun List<NutritionPlanDto>.toPlanHistory(): PlanHistory = PlanHistory.of(
    mapNotNull { dto -> dto.publishedAt?.toInstantOrNull()?.let { dto.toDomain(it) } },
)

private fun NutritionPlanDto.toDomain(publishedAt: Instant): NutritionPlanVersion {
    val items = guidelineItems?.mapNotNull { item ->
        PlanGuidelineCode.fromCode(item.code)?.let { PlanGuidelineItem.Catalog(it) }
            ?: (item.custom ?: item.code)?.takeIf { it.isNotBlank() }?.let { PlanGuidelineItem.Custom(it.trim()) }
    } ?: guidelines.filter { it.isNotBlank() }.map { text ->
        PlanGuidelineCode.fromCode(text)?.let { PlanGuidelineItem.Catalog(it) } ?: PlanGuidelineItem.Custom(text.trim())
    }
    val restrictionItems = restrictions.filter { it.isNotBlank() }.map { code ->
        PlanRestrictionCode.fromCode(code)?.let { PlanRestrictionItem.Catalog(it) } ?: PlanRestrictionItem.Legacy(code.trim())
    } + legacyRestrictions.orEmpty().filter { it.isNotBlank() }.map { PlanRestrictionItem.Legacy(it.trim()) }
    return NutritionPlanVersion(
        version = version,
        isActive = isActive,
        publishedAt = publishedAt,
        targets = (prescribed ?: proposal).toDomain(),
        guidelines = items,
        restrictions = restrictionItems,
        patientMessage = patientMessage?.takeIf { it.isNotBlank() },
    )
}
