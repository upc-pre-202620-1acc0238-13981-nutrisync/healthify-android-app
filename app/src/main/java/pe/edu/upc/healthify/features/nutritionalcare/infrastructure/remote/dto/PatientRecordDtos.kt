package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `PatientOwnRecordResource` (RM-4): solo las secciones propias del paciente que PT20 dibuja. El resto de
 * `PatientRecordResource` (identidad, series, consistencia) se ignora.
 */
@Serializable
data class PatientOwnRecordDto(
    val patientId: Long,
    val practitioner: RecordPractitionerDto? = null,
    val nextFollowUp: RecordNextFollowUpDto? = null,
    val myNumbers: RecordMyNumbersDto? = null,
    val plan: RecordPlanDto? = null,
    val referrals: List<RecordReferralDto> = emptyList(),
)

@Serializable
data class RecordPractitionerDto(
    val practitionerId: Long,
    val fullName: String? = null,
    val linkedSince: String? = null,
    val linkStatus: String? = null,
)

@Serializable
data class RecordNextFollowUpDto(
    val followUpId: Long,
    val scheduledFor: String,
)

@Serializable
data class RecordMyNumbersDto(
    val energyTargetKcal: Double? = null,
    val planVersion: Int? = null,
    val compliance: RecordComplianceDto? = null,
    val clinicalWeight: RecordClinicalWeightDto? = null,
    val weightSlopeKgPerWeek: Double? = null,
)

@Serializable
data class RecordComplianceDto(
    val from: String,
    val to: String,
    val met: Int,
    val total: Int,
)

@Serializable
data class RecordClinicalWeightDto(
    val kg: Double,
    val takenAt: String,
)

@Serializable
data class RecordPlanDto(
    val guidelines: List<RecordGuidelineDto> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val legacyRestrictions: List<String> = emptyList(),
)

@Serializable
data class RecordGuidelineDto(
    val code: String? = null,
    val custom: String? = null,
)

@Serializable
data class RecordReferralDto(
    val referralId: Long,
    val specialty: String,
    val issuedAt: String,
    val status: String? = null,
)
