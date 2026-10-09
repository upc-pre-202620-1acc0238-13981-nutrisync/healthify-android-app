package pe.edu.upc.healthify.core.database.pending

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    /** Ignora un `clientEntryId` repetido: encolar dos veces la misma operación no la duplica. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: PendingOperationEntity): Long

    @Query(
        "SELECT DISTINCT type FROM pending_operations " +
            "WHERE owner_user_id = :ownerUserId AND status = 'PENDING'",
    )
    suspend fun pendingTypes(ownerUserId: Long): List<String>

    @Query(
        "SELECT * FROM pending_operations " +
            "WHERE owner_user_id = :ownerUserId AND type = :type AND status = 'PENDING' " +
            "ORDER BY created_at_epoch_ms LIMIT :limit",
    )
    suspend fun pendingBatch(ownerUserId: Long, type: String, limit: Int): List<PendingOperationEntity>

    @Query("UPDATE pending_operations SET attempts = attempts + 1 WHERE client_entry_id IN (:clientEntryIds)")
    suspend fun incrementAttempts(clientEntryIds: List<String>)

    @Query("DELETE FROM pending_operations WHERE client_entry_id IN (:clientEntryIds)")
    suspend fun delete(clientEntryIds: List<String>)

    @Query(
        "UPDATE pending_operations SET status = 'REJECTED', rejection_code = :rejectionCode " +
            "WHERE client_entry_id = :clientEntryId",
    )
    suspend fun markRejected(clientEntryId: String, rejectionCode: String)

    @Query(
        "SELECT * FROM pending_operations WHERE owner_user_id = :ownerUserId AND type = :type " +
            "ORDER BY created_at_epoch_ms",
    )
    fun observe(ownerUserId: Long, type: String): Flow<List<PendingOperationEntity>>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE owner_user_id = :ownerUserId AND status = 'PENDING'")
    fun observePendingCount(ownerUserId: Long): Flow<Int>
}
