package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface ActiveTargetsCacheDao {

    @Query("SELECT * FROM active_targets_cache WHERE patient_id = :patientId LIMIT 1")
    suspend fun get(patientId: Long): ActiveTargetsCacheEntity?

    @Upsert
    suspend fun upsert(entity: ActiveTargetsCacheEntity)

    @Query("DELETE FROM active_targets_cache WHERE patient_id = :patientId")
    suspend fun delete(patientId: Long)
}
