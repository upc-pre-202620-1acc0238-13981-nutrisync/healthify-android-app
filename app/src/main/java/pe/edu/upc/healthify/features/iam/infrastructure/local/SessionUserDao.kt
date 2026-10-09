package pe.edu.upc.healthify.features.iam.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionUserDao {

    @Query("SELECT * FROM session_user LIMIT 1")
    fun observe(): Flow<SessionUserEntity?>

    @Query("SELECT * FROM session_user LIMIT 1")
    suspend fun get(): SessionUserEntity?

    @Upsert
    suspend fun upsert(user: SessionUserEntity)

    @Query("DELETE FROM session_user")
    suspend fun clear()
}
