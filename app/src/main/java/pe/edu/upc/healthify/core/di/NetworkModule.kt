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
     * Cliente base: idioma, logging de debug y timeouts. Lo comparten los dos clientes de abajo, así que
     * reutilizan el pool de conexiones y el dispatcher.
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

    /** Cliente autenticado: bearer en cada petición y renovación del par ante 401 (IAM-4). */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @AnonymousApi baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = baseClient.newBuilder()
        .apply { interceptors().add(1, authInterceptor) } // después de Accept-Language, antes del logging
        .authenticator(tokenRefreshAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit = retrofit(okHttpClient, json)

    /** Para los endpoints anónimos de Iam (sign-up, sign-in, token-refreshes): sin bearer ni authenticator. */
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

    // Generosos: las funciones de IA del backend tienen timeout de 20 s + un reintento, y la foto del plato
    // (PT6, multipart) sube por redes móviles lentas.
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_WRITE_TIMEOUT_SECONDS = 60L
}

@Module
@InstallIn(SingletonComponent::class)
interface NetworkBindingsModule {
    /** Declara el set para que release (sin interceptores de debug) compile con un set vacío. */
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
