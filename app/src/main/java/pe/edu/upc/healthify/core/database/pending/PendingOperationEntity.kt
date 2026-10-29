package pe.edu.upc.healthify.core.database.pending

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Operation recorded offline and waiting to be sent (diary, self weigh-ins…). Generic: [type] says which
 * `PendingOperationSender` sends it and [payload] is the JSON that feature serialized.
 *
 * [clientEntryId] is the UUID generated on the device when recording: the same one is always resent, so the
 * backend recognizes the retry and does not create a duplicate.
 */
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["owner_user_id", "type", "status"])],
)
data class PendingOperationEntity(
    @PrimaryKey
    @ColumnInfo(name = "client_entry_id")
    val clientEntryId: String,
    val type: String,
    /** The user who recorded it: it is only sent with their session (the backend answers 403 for another patient). */
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Long,
    val payload: String,
    /** The time the patient declared (ISO-8601 with offset). The backend never rewrites it. */
    @ColumnInfo(name = "local_timestamp")
    val localTimestamp: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    val attempts: Int = 0,
    /** `PENDING` or `REJECTED` (the backend did not accept it; kept so it can be shown, never retried). */
    val status: String = STATUS_PENDING,
    @ColumnInfo(name = "rejection_code")
    val rejectionCode: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_REJECTED = "REJECTED"
    }
}
