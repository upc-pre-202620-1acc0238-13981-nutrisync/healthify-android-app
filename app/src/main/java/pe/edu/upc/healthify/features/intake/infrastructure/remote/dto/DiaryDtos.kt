package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `DiaryEntryResource`. It is also the format of the offline copy of a day. */
@Serializable
data class DiaryEntryDto(
    val diaryEntryId: Long,
    val patientId: Long,
    val localTimestamp: String,
    val localDate: String? = null,
    val provenance: String,
    val photoRef: String? = null,
    val proposedReferenceFoodId: Long? = null,
    val proposedPortionGrams: Double? = null,
    val confidence: Double? = null,
    val proposedEstimatedAt: String? = null,
    val confirmedReferenceFoodId: Long? = null,
    val confirmedPortionGrams: Double? = null,
    val confirmedAt: String? = null,
    val syncState: String? = null,
    val planAdherence: String? = null,
    val isCountedTowardsTargets: Boolean = false,
    val foodName: String? = null,
    val mealGroupId: String? = null,
    val origin: String? = null,
)

/** `LogMealManuallyResource` (`planAdherence` required, `clientEntryId` for idempotency). */
@Serializable
data class ManualLogRequestDto(
    val patientId: Long,
    val localTimestamp: String,
    val referenceFoodId: Long,
    val portionGrams: Double,
    val planAdherence: String,
    val clientEntryId: String,
)

/** `PhotoConfirmationResource`: `AsProposed`, or `Adjusted` with food and grams. */
@Serializable
data class PhotoConfirmationDto(
    val kind: String,
    val referenceFoodId: Long? = null,
    val portionGrams: Double? = null,
)

/**
 * `LogMealByPhotoResource` with `analysisId`: the proposal comes from the analysis and the server ignores
 * `referenceFoodId`/`portionGrams`/`confidence` in the body; they are sent anyway because the resource declares them.
 * Without [confirmation], the entry stays "To confirm" and [planAdherence] is ignored.
 */
@Serializable
data class PhotoLogRequestDto(
    val patientId: Long,
    val localTimestamp: String,
    val referenceFoodId: Long,
    val portionGrams: Double,
    val confidence: Double,
    val analysisId: String,
    val clientEntryId: String,
    val confirmation: PhotoConfirmationDto? = null,
    val planAdherence: String? = null,
)

/** `MealGroupItemResource`. */
@Serializable
data class MealGroupItemDto(
    val referenceFoodId: Long,
    val portionGrams: Double,
    val clientEntryId: String,
)

/** `MealGroupOriginResource` (`kind = MealIdea`). */
@Serializable
data class MealGroupOriginDto(val kind: String, val mealIdeaId: String? = null)

/** `LogMealGroupResource`. */
@Serializable
data class MealGroupLogRequestDto(
    val patientId: Long,
    val localTimestamp: String,
    val items: List<MealGroupItemDto>,
    val planAdherence: String,
    val origin: MealGroupOriginDto? = null,
)

/** `EstimateConfirmationResource`. */
@Serializable
data class EstimateConfirmationRequestDto(val planAdherence: String)

/** `AdjustEstimateResource`. */
@Serializable
data class EstimateAdjustmentRequestDto(
    val referenceFoodId: Long,
    val portionGrams: Double,
    val planAdherence: String,
)

/** `PendingDiaryEntryResource` (with `confirmed`/`planAdherence`). */
@Serializable
data class PendingDiaryEntryDto(
    val clientEntryId: String,
    val localTimestamp: String,
    val provenance: String,
    val referenceFoodId: Long? = null,
    val portionGrams: Double? = null,
    val confidence: Double? = null,
    val confirmed: Boolean? = null,
    val planAdherence: String? = null,
    val photoRef: String? = null,
)

/** `SyncPendingEntriesResource`. */
@Serializable
data class SyncPendingEntriesRequestDto(val patientId: Long, val entries: List<PendingDiaryEntryDto>)

/** `SyncedEntryOutcomeResource`: `Created` · `AlreadyPresent` · `ConflictResolved` · `Rejected` (with `reason`). */
@Serializable
data class SyncedEntryOutcomeDto(
    val clientEntryId: String,
    val diaryEntryId: Long? = null,
    val outcome: String,
    val reason: String? = null,
)

/** `SyncOutcomeResource`: each item is reported separately. */
@Serializable
data class SyncOutcomeDto(
    val patientId: Long? = null,
    val entries: List<SyncedEntryOutcomeDto> = emptyList(),
)
