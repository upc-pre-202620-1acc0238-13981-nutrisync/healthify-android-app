package pe.edu.upc.healthify.features.foodcatalog.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.core.database.AppDatabase
import pe.edu.upc.healthify.features.foodcatalog.domain.repository.ReferenceFoodRepository
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.local.LocalFoodDao
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.remote.ReferenceFoodService
import pe.edu.upc.healthify.features.foodcatalog.infrastructure.repository.ReferenceFoodRepositoryImpl
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FoodCatalogProvidesModule {

    @Provides
    @Singleton
    fun provideReferenceFoodService(retrofit: Retrofit): ReferenceFoodService =
        retrofit.create(ReferenceFoodService::class.java)

    @Provides
    fun provideLocalFoodDao(db: AppDatabase): LocalFoodDao = db.localFoodDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface FoodCatalogBindingsModule {

    @Binds
    fun bindReferenceFoodRepository(impl: ReferenceFoodRepositoryImpl): ReferenceFoodRepository
}
