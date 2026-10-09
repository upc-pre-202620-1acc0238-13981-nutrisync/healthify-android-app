package pe.edu.upc.healthify.core.network

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/** Idioma en que la app muestra sus textos, como lo entiende el backend: `es` o `en`. */
fun interface AppLanguageProvider {
    fun currentLanguage(): String
}

/**
 * Idioma actual de la app: el elegido por app (per-app language, PT21.I/PR20.I) y, si no hay, el de la
 * configuración. Solo hay recursos en español (base) y en inglés, así que cualquier otro idioma se ve en español.
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
