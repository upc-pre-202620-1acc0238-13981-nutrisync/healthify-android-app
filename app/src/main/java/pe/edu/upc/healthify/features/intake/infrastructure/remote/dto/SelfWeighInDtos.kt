package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `RecordSelfWeighInResource` (IN-3: solo se pregunta `fastedState`; `sameTimeOfDay`/`sameScale` no se envían). */
@Serializable
data class RecordSelfWeighInRequestDto(
    val patientId: Long,
    val valueKg: Double,
    val localTimestamp: String,
    val fastedState: Boolean,
)

/** `SelfWeighInResource`. */
@Serializable
data class SelfWeighInDto(
    val selfWeighInId: Long,
    val patientId: Long,
    val valueKg: Double,
    val localTimestamp: String,
    val fastedState: Boolean,
    val followsProtocol: Boolean = false,
)

/** `PendingSelfWeighInResource` (IN-4). */
@Serializable
data class PendingSelfWeighInDto(
    val clientEntryId: String,
    val valueKg: Double,
    val localTimestamp: String,
    val fastedState: Boolean,
)

/** `SyncSelfWeighInsResource`. */
@Serializable
data class SyncSelfWeighInsRequestDto(
    val entries: List<PendingSelfWeighInDto>,
    val patientId: Long? = null,
)

/** `SyncedSelfWeighInOutcomeResource`: `Created`, `AlreadyPresent` o `Rejected` (con `reason`). */
@Serializable
data class SyncedSelfWeighInOutcomeDto(
    val clientEntryId: String,
    val selfWeighInId: Long? = null,
    val outcome: String,
    val reason: String? = null,
)

/** `SelfWeighInSyncOutcomeResource`: cada lectura se reporta por separado; el lote nunca falla entero. */
@Serializable
data class SelfWeighInSyncOutcomeDto(
    val patientId: Long? = null,
    val created: Int = 0,
    val alreadyPresent: Int = 0,
    val rejected: Int = 0,
    val entries: List<SyncedSelfWeighInOutcomeDto> = emptyList(),
)

/** `WeightTrendPointResource`. */
@Serializable
data class WeightTrendPointDto(
    val date: String,
    val smoothedValueKg: Double,
)

/** `WeightTrendResource` (IN-5). Sin campo de «último peso» ni «peso de hoy», a propósito. */
@Serializable
data class WeightTrendDto(
    val patientId: Long,
    val windowSize: Int,
    val lastRecalculatedAt: String,
    val points: List<WeightTrendPointDto> = emptyList(),
    val excludedReadingsCount: Int = 0,
    val changeKgOverRange: Double? = null,
    val slopeKgPerWeek: Double? = null,
    val rangeFrom: String? = null,
    val rangeTo: String? = null,
)
