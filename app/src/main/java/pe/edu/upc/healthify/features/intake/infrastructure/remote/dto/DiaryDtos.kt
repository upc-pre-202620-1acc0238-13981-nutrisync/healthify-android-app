package pe.edu.upc.healthify.features.intake.infrastructure.remote.dto

import kotlinx.serialization.Serializable

/** `DiaryEntryResource` (§5.5, IN-1/IN-6). También es el formato de la copia offline de un día. */
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

/** `LogMealManuallyResource` (IN-1 `planAdherence` requerido, IN-7 `clientEntryId`). */
@Serializable
data class ManualLogRequestDto(
    val patientId: Long,
    val localTimestamp: String,
    val referenceFoodId: Long,
    val portionGrams: Double,
    val planAdherence: String,
    val clientEntryId: String,
)

/** `PhotoConfirmationResource`: `AsProposed` o `Adjusted` con alimento y gramos (IN-2). */
@Serializable
data class PhotoConfirmationDto(
    val kind: String,
    val referenceFoodId: Long? = null,
    val portionGrams: Double? = null,
)

/**
 * `LogMealByPhotoResource` con `analysisId` (IN-7): la propuesta sale del análisis y el servidor ignora
 * `referenceFoodId`/`portionGrams`/`confidence` del cuerpo; se mandan igual porque el recurso los declara.
 * Sin [confirmation], la entrada queda «Por confirmar» y [planAdherence] se ignora.
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

/** `LogMealGroupResource` (IN-6, PT14.5). */
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

/** `PendingDiaryEntryResource` (F20, IN-1 `confirmed`/`planAdherence`). */
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

/** `SyncedEntryOutcomeResource`: `Created` · `AlreadyPresent` · `ConflictResolved` · `Rejected` (con `reason`). */
@Serializable
data class SyncedEntryOutcomeDto(
    val clientEntryId: String,
    val diaryEntryId: Long? = null,
    val outcome: String,
    val reason: String? = null,
)

/** `SyncOutcomeResource`: cada ítem se reporta por separado. */
@Serializable
data class SyncOutcomeDto(
    val patientId: Long? = null,
    val entries: List<SyncedEntryOutcomeDto> = emptyList(),
)
