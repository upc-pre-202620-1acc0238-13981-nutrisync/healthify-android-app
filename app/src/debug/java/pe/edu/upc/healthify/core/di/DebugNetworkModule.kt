package pe.edu.upc.healthify.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Solo en debug (la dependencia de logging es `debugImplementation`). Nivel HEADERS y con
 * `Authorization` redactado: los cuerpos de sign-in/token-refreshes llevan tokens y nunca se imprimen.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {
    @Provides
    @IntoSet
    @DebugInterceptors
    fun provideLoggingInterceptor(): Interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }
}
