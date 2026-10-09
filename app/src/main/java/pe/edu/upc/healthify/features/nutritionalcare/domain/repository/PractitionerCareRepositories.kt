package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.Consultation
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisChoice
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.DiagnosisSuggestion
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
 * Lado del nutricionista de NutritionalCare. Nada de esto se guarda en el teléfono: la cartera y los datos de los
 * pacientes no están disponibles sin conexión (PR1.O).
 */

/** NC-1 · `/patients/{pid}/baseline`. */
interface PatientBaselineRepository {

    /** `null` si el paciente aún no tiene datos base (`404 BaselineNotFound`, PAC-0). */
    suspend fun get(patientId: PatientId): Result<PatientBaseline?>

    /** `POST` (primera vez) · `409 BaselineAlreadyRecorded` si ya existían. */
    suspend fun record(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline>

    /** `PUT` (editar desde Resumen o EV-2). */
    suspend fun update(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline>
}

/** RM-2 · `GET /patients/{pid}/summary` (PAC-0, PAC-1, PAC-1.C). */
interface PatientSummaryRepository {
    suspend fun getSummary(patientId: PatientId): Result<PatientSummary>
}

/** RM-4 · `GET /patients/{pid}/record?days=30` con el token del profesional (PAC-3). */
interface PractitionerRecordRepository {
    suspend fun getRecord(patientId: PatientId): Result<PractitionerPatientRecord>
}

/** PAC-4 · `GET /patients/{pid}/nutrition-plans` (solo versiones publicadas). */
interface NutritionPlanRepository {
    suspend fun getHistory(patientId: PatientId): Result<PlanHistory>
}

/** NC-2 a NC-7 · la consulta guiada de 4 pasos (EV-2 a EV-5). Cada paso guardado devuelve la consulta al día. */
interface ConsultationRepository {

    /**
     * `POST /patients/{pid}/consultations`. `409 ConsultationAlreadyInProgress` si ya hay una (se reanuda) y
     * `422 BaselineRequired` sin datos base.
     */
    suspend fun start(patientId: PatientId, scheduledFollowUpId: Long?): Result<Consultation>

    /** `GET /patients/{pid}/consultations/in-progress`; `null` si no hay ninguna (404). */
    suspend fun getInProgress(patientId: PatientId): Result<Consultation?>

    /** Paso 1 · `PUT /consultations/{id}/measurement`. */
    suspend fun recordMeasurement(id: ConsultationId, measurement: NewMeasurement): Result<Consultation>

    /** Paso 2 · `POST /consultations/{id}/diagnosis-suggestion` (IA-6; nunca vacío: si no hay IA, la regla del IMC). */
    suspend fun suggestDiagnosis(id: ConsultationId): Result<DiagnosisSuggestion>

    /** Paso 2 · `PUT /consultations/{id}/diagnosis`. */
    suspend fun issueDiagnosis(id: ConsultationId, choice: DiagnosisChoice): Result<Consultation>

    /** Paso 3 · `POST /consultations/{id}/target-proposal`: [parameters] `null` = los parámetros por defecto. */
    suspend fun proposeTargets(id: ConsultationId, parameters: TargetParameters?): Result<TargetProposal>

    /** Paso 3 · `PUT /consultations/{id}/targets`. */
    suspend fun prescribeTargets(id: ConsultationId, prescription: TargetPrescription): Result<Consultation>

    /** Paso 4 · `POST /consultations/{id}/guideline-suggestions` (IA-7, o la tabla fija). */
    suspend fun suggestGuidelines(id: ConsultationId): Result<GuidelineSuggestions>

    /** Paso 4 · `PUT /consultations/{id}/publication-draft`. */
    suspend fun saveDraft(id: ConsultationId, publication: Publication): Result<Consultation>

    /**
     * Paso 4 · `POST /consultations/{id}/publication` con `Idempotency-Key`: repetirlo con la misma clave después de
     * un timeout devuelve el mismo resultado y no publica una segunda versión (EV-5.E «Volver a intentarlo»).
     */
    suspend fun publish(
        id: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation>
}
