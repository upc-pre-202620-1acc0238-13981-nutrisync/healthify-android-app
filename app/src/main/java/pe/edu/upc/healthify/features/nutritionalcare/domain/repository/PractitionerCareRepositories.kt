package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

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
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId

/*
 * Nutritionist side of NutritionalCare. None of this is stored on the phone: the caseload and patient data are
 * not available offline.
 */

/** `/patients/{pid}/baseline`. */
interface PatientBaselineRepository {

    /** `null` if the patient has no baseline yet (`404 BaselineNotFound`). */
    suspend fun get(patientId: PatientId): Result<PatientBaseline?>

    /** `POST` (first time) · `409 BaselineAlreadyRecorded` if it already existed. */
    suspend fun record(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline>

    /** `PUT` (edit from the Summary tab or the measurement step). */
    suspend fun update(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline>
}

/** `GET /patients/{pid}/summary`. */
interface PatientSummaryRepository {
    suspend fun getSummary(patientId: PatientId): Result<PatientSummary>
}

/** `GET /patients/{pid}/record?days=30` with the practitioner's token. */
interface PractitionerRecordRepository {
    suspend fun getRecord(patientId: PatientId): Result<PractitionerPatientRecord>

    /** Detail of an assessment (`GET /nutritional-assessments/{id}`). */
    suspend fun getEvaluation(assessmentId: Long): Result<EvaluationDetail>
}

/** `GET /patients/{pid}/nutrition-plans` (published versions only). */
interface NutritionPlanRepository {
    suspend fun getHistory(patientId: PatientId): Result<PlanHistory>
}

/** The 4-step guided consultation. Each saved step returns the up-to-date consultation. */
interface ConsultationRepository {

    /**
     * `POST /patients/{pid}/consultations`. `409 ConsultationAlreadyInProgress` if one exists (it is resumed) and
     * `422 BaselineRequired` without a baseline.
     */
    suspend fun start(patientId: PatientId, scheduledFollowUpId: Long?): Result<Consultation>

    /** `GET /patients/{pid}/consultations/in-progress`; `null` if there is none (404). */
    suspend fun getInProgress(patientId: PatientId): Result<Consultation?>

    /** Step 1 · `PUT /consultations/{id}/measurement`. */
    suspend fun recordMeasurement(id: ConsultationId, measurement: NewMeasurement): Result<Consultation>

    /** Step 2 · `POST /consultations/{id}/diagnosis-suggestion` (never empty: without AI, the BMI rule). */
    suspend fun suggestDiagnosis(id: ConsultationId): Result<DiagnosisSuggestion>

    /** Step 2 · `PUT /consultations/{id}/diagnosis`. */
    suspend fun issueDiagnosis(id: ConsultationId, choice: DiagnosisChoice): Result<Consultation>

    /** Step 3 · `POST /consultations/{id}/target-proposal`: [parameters] `null` = the default parameters. */
    suspend fun proposeTargets(id: ConsultationId, parameters: TargetParameters?): Result<TargetProposal>

    /** Step 3 · `PUT /consultations/{id}/targets`. */
    suspend fun prescribeTargets(id: ConsultationId, prescription: TargetPrescription): Result<Consultation>

    /** Step 4 · `POST /consultations/{id}/guideline-suggestions` (AI, or the fixed table). */
    suspend fun suggestGuidelines(id: ConsultationId): Result<GuidelineSuggestions>

    /** Step 4 · `PUT /consultations/{id}/publication-draft`. */
    suspend fun saveDraft(id: ConsultationId, publication: Publication): Result<Consultation>

    /**
     * Step 4 · `POST /consultations/{id}/publication` with `Idempotency-Key`: repeating it with the same key after
     * a timeout returns the same result and never publishes a second version ("Try again").
     */
    suspend fun publish(
        id: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation>

    /**
     * `DELETE /consultations/{id}`: discards the in-progress consultation. What was already saved (the measurement)
     * stays as history; the current diagnosis and plan do not change.
     */
    suspend fun discard(id: ConsultationId): Result<Unit>
}
