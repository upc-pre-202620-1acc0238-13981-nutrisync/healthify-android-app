package pe.edu.upc.healthify.core.network

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/** Language the app shows its texts in, as the backend understands it: `es` or `en`. */
fun interface AppLanguageProvider {
    fun currentLanguage(): String
}

/**
 * Current app language: the per-app language choice and, if there is none, the configuration's.
 * Resources exist only in Spanish (base) and English, so any other language shows Spanish.
 */
class AndroidAppLanguageProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLanguageProvider {

    override fun currentLanguage(): String {
        AndroidAppLanguageSetter.storedLanguage(context)?.let { return toSupportedLanguage(Locale.forLanguageTag(it)) }
        val appLocales = LocaleManagerCompat.getApplicationLocales(context)
        val locale = appLocales[0] ?: ConfigurationCompat.getLocales(context.resources.configuration)[0]
        return toSupportedLanguage(locale)
    }

    companion object {
        const val SPANISH = "es"
        const val ENGLISH = "en"

        fun toSupportedLanguage(locale: Locale?): String =
            if (locale?.language == ENGLISH) ENGLISH else SPANISH
    }
}
