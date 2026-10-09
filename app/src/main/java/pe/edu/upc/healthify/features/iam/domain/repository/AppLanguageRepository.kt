package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage

/** Idioma de la interfaz en este teléfono (PT21.I/PR20.I). Los datos clínicos nunca se traducen: solo la interfaz. */
interface AppLanguageRepository {

    fun current(): PreferredLanguage

    /** Aplica el idioma de inmediato en la app (lo guarda el teléfono). */
    fun apply(language: PreferredLanguage)
}
