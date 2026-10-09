package pe.edu.upc.healthify.features.intake.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import pe.edu.upc.healthify.core.database.AppDatabase
import pe.edu.upc.healthify.core.sync.PendingOperationSender
import pe.edu.upc.healthify.features.intake.domain.repository.ActiveTargetsRepository
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.repository.MealIdeasRepository
import pe.edu.upc.healthify.features.intake.domain.repository.MealPhotoRepository
import pe.edu.upc.healthify.features.intake.domain.repository.SelfWeighInRepository
import pe.edu.upc.healthify.features.intake.infrastructure.local.ActiveTargetsCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.DiaryDayCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.PendingMealPhotoDao
import pe.edu.upc.healthify.features.intake.infrastructure.local.WeightTrendCacheDao
import pe.edu.upc.healthify.features.intake.infrastructure.photo.AndroidMealPhotoEncoder
import pe.edu.upc.healthify.features.intake.infrastructure.photo.MealPhotoEncoder
import pe.edu.upc.healthify.features.intake.infrastructure.remote.ActiveTargetsService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.DiaryService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.MealIdeasService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.MealPhotoService
import pe.edu.upc.healthify.features.intake.infrastructure.remote.SelfWeighInService
import pe.edu.upc.healthify.features.intake.infrastructure.repository.ActiveTargetsRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.repository.DiaryRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.repository.MealIdeasRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.repository.MealPhotoRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.repository.SelfWeighInRepositoryImpl
import pe.edu.upc.healthify.features.intake.infrastructure.sync.DiaryEntrySyncSender
import pe.edu.upc.healthify.features.intake.infrastructure.sync.SelfWeighInSyncSender
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntakeProvidesModule {

    @Provides
    @Singleton
    fun provideActiveTargetsService(retrofit: Retrofit): ActiveTargetsService =
        retrofit.create(ActiveTargetsService::class.java)

    @Provides
    @Singleton
    fun provideDiaryService(retrofit: Retrofit): DiaryService = retrofit.create(DiaryService::class.java)

    @Provides
    @Singleton
    fun provideMealPhotoService(retrofit: Retrofit): MealPhotoService = retrofit.create(MealPhotoService::class.java)

    @Provides
    @Singleton
    fun provideMealIdeasService(retrofit: Retrofit): MealIdeasService = retrofit.create(MealIdeasService::class.java)

    @Provides
    @Singleton
    fun provideSelfWeighInService(retrofit: Retrofit): SelfWeighInService = retrofit.create(SelfWeighInService::class.java)

    @Provides
    fun provideWeightTrendCacheDao(db: AppDatabase): WeightTrendCacheDao = db.weightTrendCacheDao()

    @Provides
    fun provideActiveTargetsCacheDao(db: AppDatabase): ActiveTargetsCacheDao = db.activeTargetsCacheDao()

    @Provides
    fun provideDiaryDayCacheDao(db: AppDatabase): DiaryDayCacheDao = db.diaryDayCacheDao()

    @Provides
    fun providePendingMealPhotoDao(db: AppDatabase): PendingMealPhotoDao = db.pendingMealPhotoDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface IntakeBindingsModule {

    @Binds
    fun bindActiveTargetsRepository(impl: ActiveTargetsRepositoryImpl): ActiveTargetsRepository

    @Binds
    fun bindDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository

    @Binds
    fun bindMealPhotoRepository(impl: MealPhotoRepositoryImpl): MealPhotoRepository

    @Binds
    fun bindMealIdeasRepository(impl: MealIdeasRepositoryImpl): MealIdeasRepository

    @Binds
    fun bindSelfWeighInRepository(impl: SelfWeighInRepositoryImpl): SelfWeighInRepository

    @Binds
    fun bindMealPhotoEncoder(impl: AndroidMealPhotoEncoder): MealPhotoEncoder

    /** Comidas encoladas sin conexión → `POST /diary-entries/synchronization` (F20). */
    @Binds
    @IntoSet
    fun bindDiaryEntrySyncSender(impl: DiaryEntrySyncSender): PendingOperationSender

    /** Autopesajes encolados sin conexión → `POST /self-weigh-ins/synchronization` (IN-4). */
    @Binds
    @IntoSet
    fun bindSelfWeighInSyncSender(impl: SelfWeighInSyncSender): PendingOperationSender
}
