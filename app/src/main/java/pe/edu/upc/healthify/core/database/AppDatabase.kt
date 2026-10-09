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
 * Base de datos compartida de la app. Cada contexto aporta sus entities y DAOs desde su
 * `infrastructure/local` y los registra aquí; sus módulos Hilt exponen el DAO con `db.xxxDao()`.
 *
 * Al cambiar el esquema: subir [version], agregar una `Migration` en `DatabaseModule` (la cola de pendientes
 * no se puede perder con una migración destructiva) y versionar el JSON que se genera en `app/schemas/`.
 * Las tablas nuevas sin cambios en las existentes usan `AutoMigration`.
 *
 * - v2 (sesión 5): `active_targets_cache` (metas vigentes offline, PT3.O/PT4).
 * - v3 (sesión 6): `diary_day_cache` (PT14.O), `pending_meal_photos` (IN-7 sin conexión) y `local_food_catalog`
 *   (PT9 offline).
 * - v4 (sesión 7): `weight_trend_cache` (PT13 sin conexión).
 * - v5: `session_user.created_at` (fecha de creación de la cuenta: límite del diario PT14 y del selector del día).
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
