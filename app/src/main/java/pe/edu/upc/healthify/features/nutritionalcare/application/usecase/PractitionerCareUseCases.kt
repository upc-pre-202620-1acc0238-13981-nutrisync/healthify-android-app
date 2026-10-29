package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.EvaluationDetail
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.GuidelineSuggestions
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.NewMeasurement
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientBaseline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientSummary
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PractitionerPatientRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Publication
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetParameters
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetPrescription
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.TargetProposal
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ConsultationRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.NutritionPlanRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientBaselineRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientSummaryRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PractitionerRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import javax.inject.Inject

/** The patient summary. */
class GetPatientSummaryUseCase @Inject constructor(private val repository: PatientSummaryRepository) {
    suspend operator fun invoke(patientId: Long): Result<PatientSummary> = repository.getSummary(PatientId(patientId))
}

/** The patient record as the practitioner sees it, including the active diagnosis. */
class GetPractitionerRecordUseCase @Inject constructor(private val repository: PractitionerRecordRepository) {
    suspend operator fun invoke(patientId: Long): Result<PractitionerPatientRecord> =
        repository.getRecord(PatientId(patientId))
}

/** The plan history. */
/** One record assessment, when tapped. */
class GetEvaluationDetailUseCase @Inject constructor(private val repository: PractitionerRecordRepository) {
    suspend operator fun invoke(assessmentId: Long): Result<EvaluationDetail> = repository.getEvaluation(assessmentId)
}

class GetPlanHistoryUseCase @Inject constructor(private val repository: NutritionPlanRepository) {
    suspend operator fun invoke(patientId: Long): Result<PlanHistory> = repository.getHistory(PatientId(patientId))
}

/** The saved baseline (`null` = none yet). */
class GetPatientBaselineUseCase @Inject constructor(private val repository: PatientBaselineRepository) {
    suspend operator fun invoke(patientId: Long): Result<PatientBaseline?> = repository.get(PatientId(patientId))
}

/**
 * "Save…": records the baseline the first time and edits it afterwards. If recording returns that it already
 * existed (`409 BaselineAlreadyRecorded`, e.g. from another phone), it is saved as an edit.
 */
class SavePatientBaselineUseCase @Inject constructor(private val repository: PatientBaselineRepository) {
    suspend operator fun invoke(patientId: Long, baseline: NewBaseline, alreadyRecorded: Boolean): Result<PatientBaseline> {
        val id = PatientId(patientId)
        if (alreadyRecorded) return repository.update(id, baseline)
        val recorded = repository.record(id, baseline)
        return if (recorded.domainErrorOrNull() == DomainError.Conflict(BASELINE_ALREADY_RECORDED)) {
            repository.update(id, baseline)
        } else {
            recorded
        }
    }

    private companion object {
        const val BASELINE_ALREADY_RECORDED = "BaselineAlreadyRecorded"
    }
}

/**
 * "Start consultation" / "Continue consultation": starts the consultation or, if one is already in progress
 * (`409 ConsultationAlreadyInProgress`), resumes it. A patient never has two open consultations.
 */
class StartOrResumeConsultationUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(patientId: Long, scheduledFollowUpId: Long?): Result<Consultation> {
        val id = PatientId(patientId)
        val started = repository.start(id, scheduledFollowUpId)
        if (started.domainErrorOrNull() != DomainError.Conflict(CONSULTATION_ALREADY_IN_PROGRESS)) return started
        val inProgress = repository.getInProgress(id).getOrElse { return Result.failure(it) }
        return inProgress?.let { Result.success(it) } ?: domainFailure(DomainError.NotFound(CONSULTATION_NOT_FOUND))
    }

    private companion object {
        const val CONSULTATION_ALREADY_IN_PROGRESS = "ConsultationAlreadyInProgress"
        const val CONSULTATION_NOT_FOUND = "ConsultationNotFound"
    }
}

/**
 * Reads the in-progress consultation to **rehydrate** a step (resume after leaving or closing the app).
 * `null` if none is in progress anymore (it was published or discarded).
 */
class GetConsultationInProgressUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(patientId: Long): Result<Consultation?> = repository.getInProgress(PatientId(patientId))
}

/** "Continue to diagnosis". */
class RecordConsultationMeasurementUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, measurement: NewMeasurement): Result<Consultation> =
        repository.recordMeasurement(consultationId, measurement)
}

/** The suggestion (AI, or the BMI rule). */
class SuggestDiagnosisUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId): Result<DiagnosisSuggestion> =
        repository.suggestDiagnosis(consultationId)
}

/** "Continue to targets". */
class IssueConsultationDiagnosisUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, choice: DiagnosisChoice): Result<Consultation> =
        repository.issueDiagnosis(consultationId, choice)
}

/** Calculates (or recalculates with "Change parameters") the target proposal. */
class ProposeConsultationTargetsUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, parameters: TargetParameters? = null): Result<TargetProposal> =
        repository.proposeTargets(consultationId, parameters)
}

/** "Accept targets" / "Enter my own values" (reason required). */
class PrescribeConsultationTargetsUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, prescription: TargetPrescription): Result<Consultation> =
        repository.prescribeTargets(consultationId, prescription)
}

/** The suggested guidelines (AI, or the fixed table). */
class SuggestGuidelinesUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId): Result<GuidelineSuggestions> =
        repository.suggestGuidelines(consultationId)
}

/** Saves the draft so the choices survive even if the app is closed. */
class SavePublicationDraftUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, publication: Publication): Result<Consultation> =
        repository.saveDraft(consultationId, publication)
}

/**
 * "Publish and close consultation". The caller keeps [idempotencyKey] and reuses it on "Try again": the backend
 * returns the same result and never publishes two versions.
 */
class PublishConsultationUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(
        consultationId: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation> = repository.publish(consultationId, publication, idempotencyKey)
}

/** "Delete consultation" (opened by mistake): discards the in-progress consultation. */
class DiscardConsultationUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId): Result<Unit> = repository.discard(consultationId)
}
