package pe.edu.upc.healthify.core.network

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Cambia el idioma de la interfaz de la app (PT21.I/PR20.I): `es` o `en`. */
fun interface AppLanguageSetter {
    fun apply(languageCode: String)
}

/**
 * Idioma por app. Desde Android 13 lo guarda el sistema (`LocaleManager`, que además recrea la pantalla); antes, se
 * guarda aquí y [MainActivity][pe.edu.upc.healthify.MainActivity] lo aplica con [wrap] (PT21 recrea la pantalla).
 * Solo un código de idioma: nada personal.
 */
@Singleton
class AndroidAppLanguageSetter @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppLanguageSetter {

    override fun apply(languageCode: String) {
        val code = AndroidAppLanguageProvider.toSupportedLanguage(Locale.forLanguageTag(languageCode))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(code)
        } else {
            // commit síncrono: la Activity se recrea enseguida y debe leerlo.
            preferences(context).edit(commit = true) { putString(KEY_LANGUAGE, code) }
        }
    }

    companion object {
        private const val PREFERENCES = "app_language"
        private const val KEY_LANGUAGE = "language"

        /** El idioma guardado por la app (solo antes de Android 13; después lo sabe el sistema). */
        fun storedLanguage(context: Context): String? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                null
            } else {
                preferences(context).getString(KEY_LANGUAGE, null)
            }

        /** Contexto con el idioma elegido en la app (no hace nada desde Android 13 o si nunca se eligió). */
        fun wrap(base: Context): Context {
            val code = storedLanguage(base) ?: return base
            val locale = Locale.forLanguageTag(code)
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocale(locale)
            return base.createConfigurationContext(configuration)
        }

        private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    }
}
