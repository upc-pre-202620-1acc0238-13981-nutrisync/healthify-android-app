package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository

import kotlinx.serialization.SerializationException
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.core.network.apiCall
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
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toPlanHistory
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ConsultationService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PatientBaselineService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PractitionerPatientService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.StartConsultationRequestDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.TargetProposalRequestDto
import javax.inject.Inject

/**
 * Pasa el recurso por su mapper: un recurso que rompe una invariante del dominio (p. ej. metas en 0) se lee como
 * respuesta ilegible, igual que un JSON roto, en vez de tumbar la app.
 */
internal inline fun <T, R> Result<T>.mapResource(transform: (T) -> R): Result<R> = fold(
    onSuccess = { resource ->
        try {
            Result.success(transform(resource))
        } catch (_: IllegalArgumentException) {
            domainFailure(DomainError.Unexpected(MALFORMED_RESPONSE))
        } catch (_: SerializationException) {
            domainFailure(DomainError.Unexpected(MALFORMED_RESPONSE))
        }
    },
    onFailure = { Result.failure(it) },
)

internal const val MALFORMED_RESPONSE = "MALFORMED_RESPONSE"

/** Un 404 con [code] significa «todavía no hay» (`null`), no un error. */
private fun <T> Result<T?>.notFoundAsNull(code: String): Result<T?> =
    if (domainErrorOrNull() == DomainError.NotFound(code)) Result.success(null) else this

class PatientBaselineRepositoryImpl @Inject constructor(
    private val service: PatientBaselineService,
) : PatientBaselineRepository {

    override suspend fun get(patientId: PatientId): Result<PatientBaseline?> =
        apiCall { service.get(patientId.value) }
            .mapResource<_, PatientBaseline?> { it.toDomain() }
            .notFoundAsNull("BaselineNotFound")

    override suspend fun record(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline> =
        apiCall { service.record(patientId.value, baseline.toDto()) }.mapResource { it.toDomain() }

    override suspend fun update(patientId: PatientId, baseline: NewBaseline): Result<PatientBaseline> =
        apiCall { service.update(patientId.value, baseline.toDto()) }.mapResource { it.toDomain() }
}

class PatientSummaryRepositoryImpl @Inject constructor(
    private val service: PractitionerPatientService,
) : PatientSummaryRepository {

    override suspend fun getSummary(patientId: PatientId): Result<PatientSummary> =
        apiCall { service.getSummary(patientId.value) }.mapResource { it.toDomain() }
}

class PractitionerRecordRepositoryImpl @Inject constructor(
    private val service: PractitionerPatientService,
) : PractitionerRecordRepository {

    override suspend fun getRecord(patientId: PatientId): Result<PractitionerPatientRecord> =
        apiCall { service.getRecord(patientId.value, RECORD_DAYS) }.mapResource { it.toDomain() }

    private companion object {
        /** `PatientRecordComposer.DefaultDays`. */
        const val RECORD_DAYS = 30
    }
}

class NutritionPlanRepositoryImpl @Inject constructor(
    private val service: PractitionerPatientService,
) : NutritionPlanRepository {

    override suspend fun getHistory(patientId: PatientId): Result<PlanHistory> =
        apiCall { service.getPlans(patientId.value) }.mapResource { it.toPlanHistory() }
}

class ConsultationRepositoryImpl @Inject constructor(
    private val service: ConsultationService,
) : ConsultationRepository {

    override suspend fun start(patientId: PatientId, scheduledFollowUpId: Long?): Result<Consultation> =
        apiCall { service.start(patientId.value, StartConsultationRequestDto(scheduledFollowUpId)) }
            .mapResource { it.toDomain() }

    override suspend fun getInProgress(patientId: PatientId): Result<Consultation?> =
        apiCall { service.getInProgress(patientId.value) }
            .mapResource<_, Consultation?> { it.toDomain() }
            .notFoundAsNull("ConsultationNotFound")

    override suspend fun recordMeasurement(id: ConsultationId, measurement: NewMeasurement): Result<Consultation> =
        apiCall { service.recordMeasurement(id.value, measurement.toDto()) }.mapResource { it.toDomain() }

    override suspend fun suggestDiagnosis(id: ConsultationId): Result<DiagnosisSuggestion> =
        apiCall { service.suggestDiagnosis(id.value) }.mapResource { it.toDomain() }

    override suspend fun issueDiagnosis(id: ConsultationId, choice: DiagnosisChoice): Result<Consultation> =
        apiCall { service.issueDiagnosis(id.value, choice.toDto()) }.mapResource { it.toDomain() }

    override suspend fun proposeTargets(id: ConsultationId, parameters: TargetParameters?): Result<TargetProposal> =
        apiCall { service.proposeTargets(id.value, TargetProposalRequestDto(parameters?.toDto())) }
            .mapResource { it.toDomain() }

    override suspend fun prescribeTargets(id: ConsultationId, prescription: TargetPrescription): Result<Consultation> =
        apiCall { service.prescribeTargets(id.value, prescription.toDto()) }.mapResource { it.toDomain() }

    override suspend fun suggestGuidelines(id: ConsultationId): Result<GuidelineSuggestions> =
        apiCall { service.suggestGuidelines(id.value) }.mapResource { it.toDomain() }

    override suspend fun saveDraft(id: ConsultationId, publication: Publication): Result<Consultation> =
        apiCall { service.saveDraft(id.value, publication.toDto()) }.mapResource { it.toDomain() }

    override suspend fun publish(
        id: ConsultationId,
        publication: Publication,
        idempotencyKey: IdempotencyKey,
    ): Result<Consultation> =
        apiCall { service.publish(id.value, idempotencyKey.value, publication.toDto()) }.mapResource { it.toDomain() }
}
