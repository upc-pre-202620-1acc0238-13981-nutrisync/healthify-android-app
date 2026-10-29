package pe.edu.upc.healthify.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pe.edu.upc.healthify.BuildConfig
import pe.edu.upc.healthify.core.common.connectivity.ConnectivityObserver
import pe.edu.upc.healthify.core.network.AcceptLanguageInterceptor
import pe.edu.upc.healthify.core.network.AndroidAppLanguageProvider
import pe.edu.upc.healthify.core.network.AndroidAppLanguageSetter
import pe.edu.upc.healthify.core.network.AppLanguageProvider
import pe.edu.upc.healthify.core.network.AppLanguageSetter
import pe.edu.upc.healthify.core.network.auth.AuthInterceptor
import pe.edu.upc.healthify.core.network.auth.TokenRefreshAuthenticator
import pe.edu.upc.healthify.core.network.connectivity.AndroidConnectivityObserver
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * Base client: language, debug logging and timeouts. The two clients below share it, so
     * they reuse the connection pool and the dispatcher.
     */
    @Provides
    @Singleton
    @AnonymousApi
    fun provideAnonymousOkHttpClient(
        acceptLanguageInterceptor: AcceptLanguageInterceptor,
        @DebugInterceptors debugInterceptors: Set<@JvmSuppressWildcards Interceptor>,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(acceptLanguageInterceptor)
        .apply { debugInterceptors.forEach(::addInterceptor) }
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** Authenticated client: bearer on every request and renewal of the token pair on a 401. */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @AnonymousApi baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = baseClient.newBuilder()
        .apply { interceptors().add(1, authInterceptor) } // after Accept-Language, before logging
        .authenticator(tokenRefreshAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = retrofit(okHttpClient, json)

    /** For the anonymous Iam endpoints (sign-up, sign-in, token-refreshes): no bearer or authenticator. */
    @Provides
    @Singleton
    @AnonymousApi
    fun provideAnonymousRetrofit(@AnonymousApi okHttpClient: OkHttpClient, json: Json): Retrofit =
        retrofit(okHttpClient, json)

    private fun retrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    // Generous: the backend AI features have a 20 s timeout + one retry, and the meal photo
    // (multipart) uploads over slow mobile networks.
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_WRITE_TIMEOUT_SECONDS = 60L
}

@Module
@InstallIn(SingletonComponent::class)
interface NetworkBindingsModule {
    /** Declares the set so release (without debug interceptors) compiles with an empty set. */
    @Multibinds
    @DebugInterceptors
    fun debugInterceptors(): Set<Interceptor>

    @Binds
    fun bindAppLanguageProvider(impl: AndroidAppLanguageProvider): AppLanguageProvider

    @Binds
    fun bindAppLanguageSetter(impl: AndroidAppLanguageSetter): AppLanguageSetter

    @Binds
    fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
