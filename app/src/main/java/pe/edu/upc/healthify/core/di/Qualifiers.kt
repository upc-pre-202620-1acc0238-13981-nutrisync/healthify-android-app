package pe.edu.upc.healthify.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** DataStore de la sesión (solo texto cifrado). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

/**
 * `OkHttpClient`/`Retrofit` sin bearer ni `Authenticator`: para los endpoints anónimos de Iam (sign-up, sign-in,
 * token-refreshes). El refresh del authenticator debe usarlo para no entrar en recursión.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnonymousApi

/** Interceptores que solo existen en debug (logging). En release el set queda vacío. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DebugInterceptors
