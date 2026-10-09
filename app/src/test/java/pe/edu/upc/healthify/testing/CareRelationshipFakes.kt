package pe.edu.upc.healthify.testing

import pe.edu.upc.healthify.features.carerelationship.domain.entity.CareLink
import pe.edu.upc.healthify.features.carerelationship.domain.entity.TargetsReadStatus
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.CareLinkId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.ConsentScope
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.InvitationToken
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus

/** Token con la forma de una invitación real (base64 url-safe de 32 bytes = 43 caracteres). */
const val VALID_TOKEN = "q3Zl0_8xYb-Tn4Wc2Vd9Rk7Hs1Jm6Pa5Ue0Fi3Gt4Lo"

fun careLink(
    id: Long = 8,
    patientId: Long = 12,
    isActive: Boolean = false,
    aiProcessingGranted: Boolean = false,
) = CareLink(
    id = CareLinkId(id),
    patientId = PatientId(patientId),
    isActive = isActive,
    hasConsent = isActive,
    aiProcessingGranted = aiProcessingGranted,
    isRevoked = false,
    isDischarged = false,
)

/** Respuestas programables del vínculo; registra cada llamada. */
class FakeCareLinkRepository : CareLinkRepository {

    data class Redemption(val token: InvitationToken, val replaceActiveLink: Boolean)
    data class Grant(val careLinkId: CareLinkId, val scope: ConsentScope, val aiProcessingGranted: Boolean)

    /** Una respuesta por llamada a `getPatientLinkStatus`; la última se repite. */
    var statusResults: List<Result<PatientLinkStatus>> = listOf(Result.success(PatientLinkStatus.NO_LINK))
    var redeemResult: Result<CareLink> = Result.success(careLink())
    var grantResult: Result<CareLink>? = null
    var activeResult: Result<CareLink> = Result.success(careLink(isActive = true))
    var withdrawResult: Result<Unit> = Result.success(Unit)
    var readStatusResult: Result<TargetsReadStatus> = Result.success(readStatus(pendingVersion = null))
    var acknowledgeResult: Result<Unit> = Result.success(Unit)

    val requestedPatients = mutableListOf<PatientId>()
    val redemptions = mutableListOf<Redemption>()
    val grants = mutableListOf<Grant>()
    val withdrawals = mutableListOf<CareLinkId>()
    val pending = mutableMapOf<PatientId, CareLinkId>()
    val closed = mutableListOf<PatientId>()
    val acknowledgements = mutableListOf<Pair<CareLinkId, Int>>()

    override suspend fun getPatientLinkStatus(patientId: PatientId): Result<PatientLinkStatus> {
        val result = statusResults[minOf(requestedPatients.size, statusResults.lastIndex)]
        requestedPatients += patientId
        return result
    }

    override suspend fun rememberPendingCareLink(patientId: PatientId, careLinkId: CareLinkId) {
        pending[patientId] = careLinkId
    }

    override suspend fun pendingCareLinkId(patientId: PatientId): CareLinkId? = pending[patientId]

    override suspend fun redeemInvitation(token: InvitationToken, replaceActiveLink: Boolean): Result<CareLink> {
        redemptions += Redemption(token, replaceActiveLink)
        return redeemResult
    }

    override suspend fun grantConsent(
        patientId: PatientId,
        careLinkId: CareLinkId,
        scope: ConsentScope,
        aiProcessingGranted: Boolean,
    ): Result<CareLink> {
        grants += Grant(careLinkId, scope, aiProcessingGranted)
        return grantResult
            ?: Result.success(careLink(id = careLinkId.value, isActive = true, aiProcessingGranted = aiProcessingGranted))
    }

    override suspend fun getActiveCareLink(patientId: PatientId) = activeResult

    override suspend fun withdrawConsent(patientId: PatientId, careLinkId: CareLinkId): Result<Unit> {
        withdrawals += careLinkId
        return withdrawResult
    }

    var aiConsentResult: Result<Unit> = Result.success(Unit)
    val aiConsentChanges = mutableListOf<Pair<CareLinkId, Boolean>>()

    override suspend fun changeAiProcessingConsent(careLinkId: CareLinkId, granted: Boolean): Result<Unit> {
        aiConsentChanges += careLinkId to granted
        return aiConsentResult
    }

    override suspend fun markLinkClosed(patientId: PatientId) {
        closed += patientId
    }

    override suspend fun getTargetsReadStatus(careLinkId: CareLinkId): Result<TargetsReadStatus> = readStatusResult

    override suspend fun acknowledgeActiveTargets(careLinkId: CareLinkId, planVersion: Int): Result<Unit> {
        acknowledgements += careLinkId to planVersion
        return acknowledgeResult
    }
}

/** `targets-read-status` con [pendingVersion] esperando el acuse (o nada pendiente si es `null`). */
fun readStatus(pendingVersion: Int?, careLinkId: Long = 8) = TargetsReadStatus(
    careLinkId = CareLinkId(careLinkId),
    pendingVersion = pendingVersion,
    lastAcknowledgedVersion = pendingVersion?.minus(1)?.takeIf { it > 0 },
    hasPendingAcknowledgement = pendingVersion != null,
    lastAcknowledgedAt = null,
)
