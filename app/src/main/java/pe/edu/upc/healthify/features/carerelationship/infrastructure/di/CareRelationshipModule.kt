package pe.edu.upc.healthify.features.carerelationship.infrastructure.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.features.carerelationship.domain.repository.AiPreferencesRepository
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.AiPreferencesLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.DataStoreAiPreferencesLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.AiPreferencesService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.repository.AiPreferencesRepositoryImpl
import pe.edu.upc.healthify.features.carerelationship.domain.repository.CareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.CareLinkLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.CareRelationshipDataStore
import pe.edu.upc.healthify.features.carerelationship.infrastructure.local.DataStoreCareLinkLocalDataSource
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.CareLinkService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.repository.CareLinkRepositoryImpl
import pe.edu.upc.healthify.features.carerelationship.domain.repository.PractitionerCareLinkRepository
import pe.edu.upc.healthify.features.carerelationship.infrastructure.remote.PractitionerCareLinkService
import pe.edu.upc.healthify.features.carerelationship.infrastructure.repository.PractitionerCareLinkRepositoryImpl
import retrofit2.Retrofit
import javax.inject.Singleton

private val Context.careRelationshipDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "care_relationship",
)

@Module
@InstallIn(SingletonComponent::class)
object CareRelationshipProvidesModule {

    @Provides
    @Singleton
    fun provideCareLinkService(retrofit: Retrofit): CareLinkService = retrofit.create(CareLinkService::class.java)

    @Provides
    @Singleton
    fun providePractitionerCareLinkService(retrofit: Retrofit): PractitionerCareLinkService =
        retrofit.create(PractitionerCareLinkService::class.java)

    @Provides
    @Singleton
    fun provideAiPreferencesService(retrofit: Retrofit): AiPreferencesService =
        retrofit.create(AiPreferencesService::class.java)

    @Provides
    @Singleton
    @CareRelationshipDataStore
    fun provideCareRelationshipDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.careRelationshipDataStore
}

@Module
@InstallIn(SingletonComponent::class)
interface CareRelationshipBindingsModule {

    @Binds
    fun bindCareLinkRepository(impl: CareLinkRepositoryImpl): CareLinkRepository

    @Binds
    fun bindCareLinkLocalDataSource(impl: DataStoreCareLinkLocalDataSource): CareLinkLocalDataSource

    @Binds
    fun bindPractitionerCareLinkRepository(impl: PractitionerCareLinkRepositoryImpl): PractitionerCareLinkRepository

    @Binds
    fun bindAiPreferencesRepository(impl: AiPreferencesRepositoryImpl): AiPreferencesRepository

    @Binds
    fun bindAiPreferencesLocalDataSource(impl: DataStoreAiPreferencesLocalDataSource): AiPreferencesLocalDataSource
}
