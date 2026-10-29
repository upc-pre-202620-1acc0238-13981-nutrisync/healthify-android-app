package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

/**
 * Offline copy of the weight trend ("available from local cache"): the `WeightTrendResource`
 * as it arrived (JSON). One per patient; `404 WeightTrendNotFound` deletes it.
 */
@Entity(tableName = "weight_trend_cache")
data class WeightTrendCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)

@Dao
interface WeightTrendCacheDao {

    @Query("SELECT * FROM weight_trend_cache WHERE patient_id = :patientId LIMIT 1")
    suspend fun get(patientId: Long): WeightTrendCacheEntity?

    @Upsert
    suspend fun upsert(entity: WeightTrendCacheEntity)

    @Query("DELETE FROM weight_trend_cache WHERE patient_id = :patientId")
    suspend fun delete(patientId: Long)
}
