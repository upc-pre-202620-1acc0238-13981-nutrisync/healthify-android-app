package pe.edu.upc.healthify.features.monitoring.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationsRepository
import pe.edu.upc.healthify.features.monitoring.domain.repository.PatientMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.ConsultationsService
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.ConsultationsRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PatientMonitoringService
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.WeeklySummaryService
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.WeeklySummaryRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.domain.repository.WeeklySummaryRepository
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.PatientMonitoringRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerMonitoringRepository
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PractitionerMonitoringService
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.PractitionerMonitoringRepositoryImpl
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerAgendaRepository
import pe.edu.upc.healthify.features.monitoring.infrastructure.remote.PractitionerAgendaService
import pe.edu.upc.healthify.features.monitoring.infrastructure.repository.PractitionerAgendaRepositoryImpl
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MonitoringProvidesModule {

    @Provides
    @Singleton
    fun providePatientMonitoringService(retrofit: Retrofit): PatientMonitoringService =
        retrofit.create(PatientMonitoringService::class.java)

    @Provides
    @Singleton
    fun provideConsultationsService(retrofit: Retrofit): ConsultationsService =
        retrofit.create(ConsultationsService::class.java)

    @Provides
    @Singleton
    fun providePractitionerMonitoringService(retrofit: Retrofit): PractitionerMonitoringService =
        retrofit.create(PractitionerMonitoringService::class.java)

    @Provides
    @Singleton
    fun provideWeeklySummaryService(retrofit: Retrofit): WeeklySummaryService =
        retrofit.create(WeeklySummaryService::class.java)

    @Provides
    @Singleton
    fun providePractitionerAgendaService(retrofit: Retrofit): PractitionerAgendaService =
        retrofit.create(PractitionerAgendaService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface MonitoringBindingsModule {

    @Binds
    fun bindPractitionerMonitoringRepository(
        impl: PractitionerMonitoringRepositoryImpl,
    ): PractitionerMonitoringRepository

    @Binds
    fun bindPatientMonitoringRepository(impl: PatientMonitoringRepositoryImpl): PatientMonitoringRepository

    @Binds
    fun bindConsultationsRepository(impl: ConsultationsRepositoryImpl): ConsultationsRepository

    @Binds
    fun bindWeeklySummaryRepository(impl: WeeklySummaryRepositoryImpl): WeeklySummaryRepository

    @Binds
    fun bindPractitionerAgendaRepository(impl: PractitionerAgendaRepositoryImpl): PractitionerAgendaRepository
}
