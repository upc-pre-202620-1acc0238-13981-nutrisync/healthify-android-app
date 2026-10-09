package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.di

import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PatientRecordService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PatientRecordRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PlanVersionRepository
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PlanVersionService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PlanVersionRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ConsultationRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.NutritionPlanRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientBaselineRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientSummaryRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PractitionerRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ConsultationService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PatientBaselineService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PractitionerPatientService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.ConsultationRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.NutritionPlanRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PatientBaselineRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PatientSummaryRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.PractitionerRecordRepositoryImpl
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.ReviewInboxRepository
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.ReviewItemService
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository.ReviewInboxRepositoryImpl
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NutritionalCareProvidesModule {

    @Provides
    @Singleton
    fun providePlanVersionService(retrofit: Retrofit): PlanVersionService =
        retrofit.create(PlanVersionService::class.java)

    @Provides
    @Singleton
    fun providePatientRecordService(retrofit: Retrofit): PatientRecordService =
        retrofit.create(PatientRecordService::class.java)

    @Provides
    @Singleton
    fun providePatientBaselineService(retrofit: Retrofit): PatientBaselineService =
        retrofit.create(PatientBaselineService::class.java)

    @Provides
    @Singleton
    fun provideConsultationService(retrofit: Retrofit): ConsultationService =
        retrofit.create(ConsultationService::class.java)

    @Provides
    @Singleton
    fun providePractitionerPatientService(retrofit: Retrofit): PractitionerPatientService =
        retrofit.create(PractitionerPatientService::class.java)

    @Provides
    @Singleton
    fun provideReviewItemService(retrofit: Retrofit): ReviewItemService =
        retrofit.create(ReviewItemService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface NutritionalCareBindingsModule {

    @Binds
    fun bindPlanVersionRepository(impl: PlanVersionRepositoryImpl): PlanVersionRepository

    @Binds
    fun bindPatientRecordRepository(impl: PatientRecordRepositoryImpl): PatientRecordRepository

    @Binds
    fun bindPatientBaselineRepository(impl: PatientBaselineRepositoryImpl): PatientBaselineRepository

    @Binds
    fun bindPatientSummaryRepository(impl: PatientSummaryRepositoryImpl): PatientSummaryRepository

    @Binds
    fun bindPractitionerRecordRepository(impl: PractitionerRecordRepositoryImpl): PractitionerRecordRepository

    @Binds
    fun bindNutritionPlanRepository(impl: NutritionPlanRepositoryImpl): NutritionPlanRepository

    @Binds
    fun bindConsultationRepository(impl: ConsultationRepositoryImpl): ConsultationRepository

    @Binds
    fun bindReviewInboxRepository(impl: ReviewInboxRepositoryImpl): ReviewInboxRepository
}
