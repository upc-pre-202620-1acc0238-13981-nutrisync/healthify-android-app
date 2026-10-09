package pe.edu.upc.healthify.features.iam.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pe.edu.upc.healthify.core.database.AppDatabase
import pe.edu.upc.healthify.core.di.AnonymousApi
import pe.edu.upc.healthify.core.network.auth.TokenRefresher
import pe.edu.upc.healthify.core.sync.ActiveUserIdProvider
import pe.edu.upc.healthify.features.iam.domain.repository.AccountRepository
import pe.edu.upc.healthify.features.iam.domain.repository.AuthenticationRepository
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import pe.edu.upc.healthify.features.iam.domain.repository.UserRepository
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionUserDao
import pe.edu.upc.healthify.features.iam.infrastructure.remote.AuthenticationService
import pe.edu.upc.healthify.features.iam.infrastructure.remote.RetrofitTokenRefresher
import pe.edu.upc.healthify.features.iam.infrastructure.remote.SessionService
import pe.edu.upc.healthify.features.iam.infrastructure.repository.AccountRepositoryImpl
import pe.edu.upc.healthify.features.iam.infrastructure.repository.AuthenticationRepositoryImpl
import pe.edu.upc.healthify.features.iam.infrastructure.repository.SessionRepositoryImpl
import pe.edu.upc.healthify.features.iam.infrastructure.repository.UserRepositoryImpl
import pe.edu.upc.healthify.features.iam.infrastructure.local.AndroidAppLanguageRepository
import pe.edu.upc.healthify.features.iam.domain.repository.AppLanguageRepository
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IamProvidesModule {

    @Provides
    @Singleton
    fun provideAuthenticationService(@AnonymousApi retrofit: Retrofit): AuthenticationService =
        retrofit.create(AuthenticationService::class.java)

    @Provides
    @Singleton
    fun provideSessionService(retrofit: Retrofit): SessionService = retrofit.create(SessionService::class.java)

    @Provides
    fun provideSessionUserDao(db: AppDatabase): SessionUserDao = db.sessionUserDao()
}

@Module
@InstallIn(SingletonComponent::class)
interface IamBindingsModule {

    @Binds
    fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    fun bindAuthenticationRepository(impl: AuthenticationRepositoryImpl): AuthenticationRepository

    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    fun bindAppLanguageRepository(impl: AndroidAppLanguageRepository): AppLanguageRepository

    @Binds
    fun bindTokenRefresher(impl: RetrofitTokenRefresher): TokenRefresher

    @Binds
    fun bindActiveUserIdProvider(impl: SessionLocalDataSource): ActiveUserIdProvider
}
