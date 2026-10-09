package pe.edu.upc.healthify.features.carerelationship.infrastructure.repository

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.CareLinkLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toGrantDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toPatientLinkStatus
import pe.edu.upc.healthify.features.carerelationship.infrastructure.mapper.toRedeemDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.CareLinkService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.AcknowledgeActiveTargetsRequestDto
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.dto.ChangeAiProcessingConsentRequestDto
import javax.inject.Inject

/**
 * DECISIÓN S5/PT2.1: `GET /patients/{pid}/care-links/active` solo devuelve vínculos con consentimiento, y el backend
 * no expone el vínculo pendiente por paciente. El pendiente se detecta con el `careLinkId` que PT1 recuerda al
 * canjear la invitación y `GET /care-links/{careLinkId}`. Si el dispositivo no lo recuerda (reinstalación, otro
 * teléfono), el paciente va a PT1. Pendiente del backend: una lectura del vínculo sin cerrar del paciente.
 *
 * Cada escritura deja el estado local del vínculo al día, para que S5 decida bien también sin conexión.
 */
class CareLinkRepositoryImpl @Inject constructor(
    private val service: CareLinkService,
    private val local: CareLinkLocalDataSource,
) : CareLinkRepository {

    override suspend fun getPatientLinkStatus(patientId: PatientId): Result<PatientLinkStatus> {
        val pid = patientId.value
        val active = apiCall { service.getActiveCareLink(pid) }
        val status = active.fold(
            onSuccess = { it.toPatientLinkStatus() },
            onFailure = { failure ->
                if (active.domainErrorOrNull() !is DomainError.NotFound) return offlineFallback(pid, failure)
                pendingStatus(pid).getOrElse { return offlineFallback(pid, it) }
            },
        )
        saveStatus(pid, status)
        return Result.success(status)
    }

    override suspend fun rememberPendingCareLink(patientId: PatientId, careLinkId: CareLinkId) {
        local.savePendingCareLinkId(patientId.value, careLinkId.value)
        local.saveStatus(patientId.value, PatientLinkStatus.PENDING_CONSENT)
    }

    override suspend fun pendingCareLinkId(patientId: PatientId): CareLinkId? =
        local.pendingCareLinkId(patientId.value)?.let(::CareLinkId)

    override suspend fun redeemInvitation(token: InvitationToken, replaceActiveLink: Boolean): Result<CareLink> =
        apiCall { service.redeemInvitation(token.toRedeemDto(replaceActiveLink)) }.map { it.toDomain() }

    override suspend fun grantConsent(
        patientId: PatientId,
        careLinkId: CareLinkId,
        scope: ConsentScope,
        aiProcessingGranted: Boolean,
    ): Result<CareLink> {
        val result = apiCall { service.grantConsent(careLinkId.value, scope.toGrantDto(aiProcessingGranted)) }
            .map { it.toDomain() }
        val error = result.domainErrorOrNull()
        when {
            error == null -> saveStatus(patientId.value, result.getOrThrow().patientStatus)
            // F6 casos 3–5: el vínculo ya estaba activo o ya se cerró; deja de estar pendiente en el dispositivo.
            error is DomainError.Conflict && error.code == CODE_CONSENT_ALREADY_GRANTED ->
                saveStatus(patientId.value, PatientLinkStatus.ACTIVE)
            error is DomainError.Conflict && error.code in CLOSED_LINK_CODES ->
                saveStatus(patientId.value, PatientLinkStatus.NO_LINK)
        }
        return result
    }

    override suspend fun getActiveCareLink(patientId: PatientId): Result<CareLink> =
        apiCall { service.getActiveCareLink(patientId.value) }.map { it.toDomain() }

    override suspend fun withdrawConsent(patientId: PatientId, careLinkId: CareLinkId): Result<Unit> =
        apiCall { service.withdrawConsent(careLinkId.value) }.onSuccess { markLinkClosed(patientId) }

    override suspend fun changeAiProcessingConsent(careLinkId: CareLinkId, granted: Boolean): Result<Unit> =
        apiCall { service.changeAiProcessingConsent(careLinkId.value, ChangeAiProcessingConsentRequestDto(granted)) }

    override suspend fun markLinkClosed(patientId: PatientId) {
        saveStatus(patientId.value, PatientLinkStatus.NO_LINK)
    }

    override suspend fun getTargetsReadStatus(careLinkId: CareLinkId): Result<TargetsReadStatus> =
        apiCall { service.getTargetsReadStatus(careLinkId.value) }.map { it.toDomain() }

    override suspend fun acknowledgeActiveTargets(careLinkId: CareLinkId, planVersion: Int): Result<Unit> =
        apiCall {
            service.acknowledgeActiveTargets(careLinkId.value, AcknowledgeActiveTargetsRequestDto(planVersion))
        }.map { }

    private suspend fun saveStatus(patientId: Long, status: PatientLinkStatus) {
        if (status != PatientLinkStatus.PENDING_CONSENT) local.clearPendingCareLinkId(patientId)
        local.saveStatus(patientId, status)
    }

    /** Sin vínculo con consentimiento: ¿hay uno recién canjeado esperando el consentimiento? */
    private suspend fun pendingStatus(patientId: Long): Result<PatientLinkStatus> {
        val careLinkId = local.pendingCareLinkId(patientId) ?: return Result.success(PatientLinkStatus.NO_LINK)
        val link = apiCall { service.getCareLink(careLinkId) }
        return when (link.domainErrorOrNull()) {
            null -> link.map { it.toPatientLinkStatus() }
            // El vínculo ya no existe o no es de este paciente.
            is DomainError.NotFound, is DomainError.Forbidden -> Result.success(PatientLinkStatus.NO_LINK)
            else -> link.map { it.toPatientLinkStatus() }
        }
    }

    /** Sin conexión vale el último estado conocido; cualquier otro error se propaga. */
    private suspend fun offlineFallback(patientId: Long, failure: Throwable): Result<PatientLinkStatus> {
        val result = Result.failure<PatientLinkStatus>(failure)
        if (result.domainErrorOrNull() != DomainError.Network) return result
        return local.lastKnownStatus(patientId)?.let { Result.success(it) } ?: result
    }

    private companion object {
        const val CODE_CONSENT_ALREADY_GRANTED = "ConsentAlreadyGranted"
        val CLOSED_LINK_CODES = setOf("CareLinkAlreadyRevoked", "DischargedLinkCannotBeReactivated")
    }
}
