package pe.edu.upc.healthify.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Sends the current app language in `Accept-Language` on every request. The backend localizes ProblemDetails
 * and generates AI texts in that language (es/en).
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
