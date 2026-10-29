package pe.edu.upc.healthify.core.database

import androidx.room3.AutoMigration
import androidx.room3.Database
import androidx.room3.RoomDatabase
import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import pe.edu.upc.healthify.core.database.pending.PendingOperationEntity
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodDao
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodEntity
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserDao
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserEntity
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheEntity
import pe.edu.upc.healthify.features.intake.infrastructure.local.PendingMealPhotoDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.PendingMealPhotoEntity
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheEntity

/**
 * Shared app database. Each context contributes its entities and DAOs from its
 * `infrastructure/local` and registers them here; its Hilt modules expose the DAO with `db.xxxDao()`.
 *
 * When the schema changes: bump [version], add a `Migration` in `DatabaseModule` (the pending queue
 * must never be lost to a destructive migration) and commit the JSON generated in `app/schemas/`.
 * New tables that leave the existing ones unchanged use `AutoMigration`.
 *
 * - v2: `active_targets_cache` (current targets offline).
 * - v3: `diary_day_cache`, `pending_meal_photos` (meal photos taken offline) and `local_food_catalog`
 *   (food search offline).
 * - v4: `weight_trend_cache` (weight trend offline).
 * - v5: `session_user.created_at` (account creation date: lower bound of the diary and the day picker).
 */
@Database(
    entities = [
        PendingOperationEntity::class,
        SessionUserEntity::class,
        ActiveTargetsCacheEntity::class,
        DiaryDayCacheEntity::class,
        PendingMealPhotoEntity::class,
        LocalFoodEntity::class,
        WeightTrendCacheEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun sessionUserDao(): SessionUserDao
    abstract fun activeTargetsCacheDao(): ActiveTargetsCacheDao
    abstract fun diaryDayCacheDao(): DiaryDayCacheDao
    abstract fun pendingMealPhotoDao(): PendingMealPhotoDao
    abstract fun localFoodDao(): LocalFoodDao
    abstract fun weightTrendCacheDao(): WeightTrendCacheDao

    companion object {
        const val NAME = "healthify.db"
    }
}
