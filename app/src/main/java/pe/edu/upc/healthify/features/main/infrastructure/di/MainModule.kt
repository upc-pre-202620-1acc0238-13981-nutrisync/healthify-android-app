package pe.edu.upc.healthify.features.main.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.features.main.domain.repository.DataExportRepository
import pe.edu.upc.healthify.features.main.infrastructure.file.ContentResolverDocumentWriter
import pe.edu.upc.healthify.features.main.infrastructure.file.DocumentWriter
import pe.edu.upc.healthify.features.main.infrastructure.remote.DataExportService
import pe.edu.upc.healthify.features.main.infrastructure.repository.DataExportRepositoryImpl
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MainProvidesModule {

    @Provides
    @Singleton
    fun provideDataExportService(retrofit: Retrofit): DataExportService =
        retrofit.create(DataExportService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface MainBindingsModule {

    @Binds
    fun bindDataExportRepository(impl: DataExportRepositoryImpl): DataExportRepository

    @Binds
    fun bindDocumentWriter(impl: ContentResolverDocumentWriter): DocumentWriter
}
