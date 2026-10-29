package pe.edu.upc.healthify.core.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Session DataStore (ciphertext only). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

/**
 * `OkHttpClient`/`Retrofit` without bearer or `Authenticator`: for the anonymous Iam endpoints (sign-up, sign-in,
 * token-refreshes). The authenticator's refresh must use it to avoid recursion.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnonymousApi

/** Interceptors that only exist in debug (logging). In release the set is empty. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DebugInterceptors
