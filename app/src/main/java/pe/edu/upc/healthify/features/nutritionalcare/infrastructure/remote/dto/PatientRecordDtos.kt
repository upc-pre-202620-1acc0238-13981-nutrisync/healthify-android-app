package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/**
 * `PatientOwnRecordResource`: only the patient's own sections the record screen draws. The rest of
 * `PatientRecordResource` (identity, series, consistency) is ignored.
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
    val reason: String? = null,
    val closedAt: String? = null,
)
