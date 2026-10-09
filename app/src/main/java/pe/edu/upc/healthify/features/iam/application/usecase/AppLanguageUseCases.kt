package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AppLanguageRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import javax.inject.Inject

/** PT21 «Idioma · Español»: el idioma con el que se ve la app ahora. */
class GetAppLanguageUseCase @Inject constructor(
    private val appLanguage: AppLanguageRepository,
) {
    operator fun invoke(): PreferredLanguage = appLanguage.current()
}

/**
 * PT21.I: aplica el idioma de inmediato en el teléfono (solo la interfaz; los datos clínicos no se traducen).
 * Guardarlo en la cuenta es aparte ([ChangePreferredLanguageUseCase]): sin red la app igual cambia de idioma.
 *
 * @return `false` si ya estaba en ese idioma (no hay nada que aplicar).
 */
class ApplyAppLanguageUseCase @Inject constructor(
    private val appLanguage: AppLanguageRepository,
) {
    operator fun invoke(language: PreferredLanguage): Boolean {
        if (appLanguage.current() == language) return false
        appLanguage.apply(language)
        return true
    }
}
