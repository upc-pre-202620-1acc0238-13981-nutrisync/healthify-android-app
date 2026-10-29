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

    /** When the current copy was saved (epoch ms), or `null` if there is none. */
    @Query("SELECT MIN(saved_at) FROM local_food_catalog")
    suspend fun savedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<LocalFoodEntity>)

    @Query("DELETE FROM local_food_catalog")
    suspend fun deleteAll()

    /** Replaces the whole copy in one transaction: it is never left half written. */
    @Transaction
    suspend fun replaceAll(foods: List<LocalFoodEntity>) {
        deleteAll()
        insertAll(foods)
    }
}
