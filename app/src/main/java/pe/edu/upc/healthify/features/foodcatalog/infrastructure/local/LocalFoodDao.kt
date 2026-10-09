package pe.edu.upc.healthify.features.foodcatalog.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface LocalFoodDao {

    @Query("SELECT * FROM local_food_catalog ORDER BY position")
    suspend fun getAll(): List<LocalFoodEntity>

    /** Cuándo se guardó la copia actual (epoch ms), o `null` si no hay. */
    @Query("SELECT MIN(saved_at) FROM local_food_catalog")
    suspend fun savedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<LocalFoodEntity>)

    @Query("DELETE FROM local_food_catalog")
    suspend fun deleteAll()

    /** Reemplaza la copia entera en una transacción: nunca queda a medias. */
    @Transaction
    suspend fun replaceAll(foods: List<LocalFoodEntity>) {
        deleteAll()
        insertAll(foods)
    }
}
