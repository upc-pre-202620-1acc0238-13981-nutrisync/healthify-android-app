package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper

import pe.edu.upc.healthify.core.network.toInstantOrNull
import pe.edu.upc.healthify.core.network.toLocalDateOrNull
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.MyNumbers
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordClinicalWeight
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordCompliance
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordGuideline
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordNextFollowUp
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordPlan
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordPractitioner
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.RecordReferral
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.PatientOwnRecordDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordMyNumbersDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordPlanDto
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto.RecordReferralDto
import kotlin.math.roundToInt

/**
 * PT20 «una sección que falla queda vacía sin tumbar el resto»: cada sección se lee por separado y la que no se puede
 * interpretar queda vacía (`null`, lista vacía) en vez de hacer fallar todo el expediente.
 */
fun PatientOwnRecordDto.toDomain(): PatientOwnRecord = PatientOwnRecord(
    practitioner = practitioner?.let { dto ->
        RecordPractitioner(
            fullName = dto.fullName?.trim()?.takeIf { it.isNotEmpty() },
            linkedSince = dto.linkedSince?.toInstantOrNull(),
            isActive = dto.linkStatus == LINK_STATUS_ACTIVE,
        )
    },
    nextFollowUp = nextFollowUp?.let { dto ->
        val scheduledFor = dto.scheduledFor.toInstantOrNull()
        if (scheduledFor == null || dto.followUpId <= 0) null else RecordNextFollowUp(dto.followUpId, scheduledFor)
    },
    myNumbers = myNumbers?.toDomain() ?: MyNumbers.Empty,
    plan = plan?.toDomainOrNull(),
    referrals = referrals.mapNotNull { it.toDomainOrNull() }.sortedByDescending { it.issuedAt },
)

/** Un número ilegible se omite (queda «—»), los demás se muestran. */
fun RecordMyNumbersDto.toDomain(): MyNumbers = MyNumbers(
    energyTargetKcal = energyTargetKcal?.roundToInt()?.takeIf { it > 0 },
    planVersion = planVersion?.takeIf { it > 0 },
    compliance = compliance?.let { dto ->
        val from = dto.from.toLocalDateOrNull()
        val to = dto.to.toLocalDateOrNull()
        if (from == null || to == null || to.isBefore(from) || dto.total < 0 || dto.met !in 0..dto.total) {
            null
        } else {
            RecordCompliance(from, to, dto.met, dto.total)
        }
    },
    clinicalWeight = clinicalWeight?.let { dto ->
        val takenAt = dto.takenAt.toInstantOrNull()
        if (takenAt == null || dto.kg <= 0) null else RecordClinicalWeight(dto.kg, takenAt)
    },
    weightSlopeKgPerWeek = weightSlopeKgPerWeek,
)

fun RecordPlanDto.toDomainOrNull(): RecordPlan? {
    val plan = RecordPlan(
        guidelines = guidelines
            .filter { !it.code.isNullOrBlank() || !it.custom.isNullOrBlank() }
            .map { RecordGuideline(it.code?.trim(), it.custom?.trim()) },
        restrictions = restrictions.map(String::trim).filter(String::isNotEmpty),
        legacyRestrictions = legacyRestrictions.map(String::trim).filter(String::isNotEmpty),
    )
    return plan.takeUnless { it.isEmpty }
}

fun RecordReferralDto.toDomainOrNull(): RecordReferral? {
    val issuedAt = issuedAt.toInstantOrNull() ?: return null
    val specialty = specialty.trim().takeIf { it.isNotEmpty() } ?: return null
    return RecordReferral(
        id = referralId,
        specialty = specialty,
        issuedAt = issuedAt,
        // Sin estado (recurso anterior a RM-4) = abierta, como el default del backend.
        isOpen = status == null || status == REFERRAL_STATUS_OPEN,
    )
}

private const val LINK_STATUS_ACTIVE = "Active"
private const val REFERRAL_STATUS_OPEN = "Open"
