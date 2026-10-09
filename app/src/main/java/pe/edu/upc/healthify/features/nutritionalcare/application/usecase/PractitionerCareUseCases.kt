package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
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
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ConsultationRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.NutritionPlanRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientBaselineRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientSummaryRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PractitionerRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.ConsultationId
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.IdempotencyKey
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import javax.inject.Inject

/** PAC-0 / PAC-1 / PAC-1.C · el resumen del paciente (RM-2). */
class GetPatientSummaryUseCase @Inject constructor(private val repository: PatientSummaryRepository) {
    suspend operator fun invoke(patientId: Long): Result<PatientSummary> = repository.getSummary(PatientId(patientId))
}

/** PAC-3 · el expediente del paciente visto por el profesional (RM-4, con el diagnóstico activo). */
class GetPractitionerRecordUseCase @Inject constructor(private val repository: PractitionerRecordRepository) {
    suspend operator fun invoke(patientId: Long): Result<PractitionerPatientRecord> =
        repository.getRecord(PatientId(patientId))
}

/** PAC-4 · el historial del plan. */
class GetPlanHistoryUseCase @Inject constructor(private val repository: NutritionPlanRepository) {
    suspend operator fun invoke(patientId: Long): Result<PlanHistory> = repository.getHistory(PatientId(patientId))
}

/** EV-1 · los datos base guardados (`null` = todavía no tiene). */
class GetPatientBaselineUseCase @Inject constructor(private val repository: PatientBaselineRepository) {
    suspend operator fun invoke(patientId: Long): Result<PatientBaseline?> = repository.get(PatientId(patientId))
}

/**
 * EV-1 · «Guardar…»: registra los datos base la primera vez y los edita después. Si al registrar el backend dice que
 * ya existían (`409 BaselineAlreadyRecorded`, p. ej. desde otro teléfono), se guardan como edición.
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
 * PAC-1 «Iniciar consulta» / PAC-1.C «Continuar consulta»: inicia la consulta o, si ya hay una en curso
 * (`409 ConsultationAlreadyInProgress`), la reanuda. Nunca hay dos consultas abiertas para un paciente.
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
 * EV-2 a EV-5 · lee la consulta en curso para **re-hidratar** el paso (reanudar después de salir o de cerrar la app).
 * `null` si ya no hay ninguna en curso (se publicó o se descartó).
 */
class GetConsultationInProgressUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(patientId: Long): Result<Consultation?> = repository.getInProgress(PatientId(patientId))
}

/** EV-2 «Continuar a diagnóstico». */
class RecordConsultationMeasurementUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, measurement: NewMeasurement): Result<Consultation> =
        repository.recordMeasurement(consultationId, measurement)
}

/** EV-3 · la sugerencia (IA-6, o la regla del IMC). */
class SuggestDiagnosisUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId): Result<DiagnosisSuggestion> =
        repository.suggestDiagnosis(consultationId)
}

/** EV-3 «Continuar a metas». */
class IssueConsultationDiagnosisUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, choice: DiagnosisChoice): Result<Consultation> =
        repository.issueDiagnosis(consultationId, choice)
}

/** EV-4 · calcula (o recalcula con «Cambiar parámetros») la propuesta de metas. */
class ProposeConsultationTargetsUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, parameters: TargetParameters? = null): Result<TargetProposal> =
        repository.proposeTargets(consultationId, parameters)
}

/** EV-4 «Aceptar metas» / «Escribir mis propios valores» (con razón obligatoria). */
class PrescribeConsultationTargetsUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, prescription: TargetPrescription): Result<Consultation> =
        repository.prescribeTargets(consultationId, prescription)
}

/** EV-5 · las indicaciones sugeridas (IA-7, o la tabla fija). */
class SuggestGuidelinesUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId): Result<GuidelineSuggestions> =
        repository.suggestGuidelines(consultationId)
}

/** EV-5 · guarda el borrador para que lo elegido no se pierda aunque se cierre la app. */
class SavePublicationDraftUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(consultationId: ConsultationId, publication: Publication): Result<Consultation> =
        repository.saveDraft(consultationId, publication)
}

/**
 * EV-5 «Publicar y cerrar consulta». El que llama conserva [idempotencyKey] y lo reusa en «Volver a intentarlo»
 * (EV-5.E): el backend devuelve el mismo resultado y no publica dos versiones.
 */
class PublishConsultationUseCase @Inject constructor(private val repository: ConsultationRepository) {
    suspend operator fun invoke(
        consultationId: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation> = repository.publish(consultationId, publication, idempotencyKey)
}
