package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Copia offline de un día del diario (PT14.O): la lista de `DiaryEntryResource` tal como llegó (JSON). Así sin
 * conexión se ve lo ya registrado y los textos se arman en el idioma que tenga la app al leerla.
 */
@Entity(tableName = "diary_day_cache", primaryKeys = ["patient_id", "date"])
data class DiaryDayCacheEntity(
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    /** `2026-09-09`. */
    val date: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "saved_at")
    val savedAtEpochMillis: Long,
)

@Dao
interface DiaryDayCacheDao {

    @Query("SELECT * FROM diary_day_cache WHERE patient_id = :patientId AND date = :date LIMIT 1")
    suspend fun get(patientId: Long, date: String): DiaryDayCacheEntity?

    @Upsert
    suspend fun upsert(entity: DiaryDayCacheEntity)

    /** Solo se guardan los últimos días: los más viejos no se consultan sin conexión. */
    @Query("DELETE FROM diary_day_cache WHERE patient_id = :patientId AND date < :oldestDate")
    suspend fun deleteOlderThan(patientId: Long, oldestDate: String)
}

/**
 * Foto del plato tomada sin conexión, esperando para analizarse (IN-7). El archivo vive en `cacheDir` (sin EXIF:
 * se reencodó al capturarla) y se borra al analizarla o descartarla. Solo el dueño la ve.
 */
@Entity(tableName = "pending_meal_photos")
data class PendingMealPhotoEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Long,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    /** Momento de la captura (ISO-8601 con offset): el «¿Cuándo comiste?» propuesto. */
    @ColumnInfo(name = "captured_at")
    val capturedAt: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
)

@Dao
interface PendingMealPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingMealPhotoEntity)

    @Query("SELECT * FROM pending_meal_photos WHERE owner_user_id = :ownerUserId ORDER BY created_at_epoch_ms")
    fun observe(ownerUserId: Long): Flow<List<PendingMealPhotoEntity>>

    @Query("SELECT * FROM pending_meal_photos WHERE owner_user_id = :ownerUserId AND id = :id LIMIT 1")
    suspend fun get(ownerUserId: Long, id: String): PendingMealPhotoEntity?

    @Query("DELETE FROM pending_meal_photos WHERE file_path = :filePath")
    suspend fun deleteByPath(filePath: String)
}
