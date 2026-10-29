package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** UI language on this phone. Clinical data is never translated: only the UI. */
interface AppLanguageRepository {

    fun current(): PreferredLanguage

    /** Applies the language in the app right away (the phone stores it). */
    fun apply(language: PreferredLanguage)
}
