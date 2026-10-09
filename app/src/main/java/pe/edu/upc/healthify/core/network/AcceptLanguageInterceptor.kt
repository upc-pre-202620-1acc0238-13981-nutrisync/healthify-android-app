package pe.edu.upc.healthify.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Envía el idioma actual de la app en `Accept-Language` en cada request. El backend localiza los ProblemDetails
 * y genera los textos de IA en ese idioma (es/en).
 */
class AcceptLanguageInterceptor @Inject constructor(
    private val languageProvider: AppLanguageProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER, languageProvider.currentLanguage())
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER = "Accept-Language"
    }
}
