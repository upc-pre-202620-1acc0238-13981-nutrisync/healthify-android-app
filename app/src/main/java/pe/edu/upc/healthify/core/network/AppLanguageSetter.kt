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

/** Changes the app UI language: `es` or `en`. */
fun interface AppLanguageSetter {
    fun apply(languageCode: String)
}

/**
 * Per-app language. From Android 13 the system stores it (`LocaleManager`, which also recreates the screen);
 * before that it is stored here and [MainActivity][pe.edu.upc.healthify.MainActivity] applies it with [wrap]
 * (the settings screen recreates itself). Only a language code: nothing personal.
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
            // Synchronous commit: the Activity is recreated right away and must read it.
            preferences(context).edit(commit = true) { putString(KEY_LANGUAGE, code) }
        }
    }

    companion object {
        private const val PREFERENCES = "app_language"
        private const val KEY_LANGUAGE = "language"

        /** The language stored by the app (only before Android 13; after that the system knows it). */
        fun storedLanguage(context: Context): String? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                null
            } else {
                preferences(context).getString(KEY_LANGUAGE, null)
            }

        /** Context with the language chosen in the app (no-op from Android 13 on or if none was ever chosen). */
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
