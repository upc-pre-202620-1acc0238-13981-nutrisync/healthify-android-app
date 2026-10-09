package pe.edu.upc.healthify.core.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import pe.edu.upc.healthify.core.database.AppDatabase
import pe.edu.upc.healthify.core.database.pending.PendingOperationDao
import pe.edu.upc.healthify.core.sync.PendingOperationSender
import pe.edu.upc.healthify.core.sync.SyncScheduler
import pe.edu.upc.healthify.core.sync.WorkManagerSyncScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun providePendingOperationDao(db: AppDatabase): PendingOperationDao = db.pendingOperationDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)
}

@Module
@InstallIn(SingletonComponent::class)
interface SyncBindingsModule {
    @Binds
    fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    /** Cada feature aporta su sender con `@Binds @IntoSet`; mientras no haya ninguno, el set queda vacío. */
    @Multibinds
    fun pendingOperationSenders(): Set<PendingOperationSender>
}
