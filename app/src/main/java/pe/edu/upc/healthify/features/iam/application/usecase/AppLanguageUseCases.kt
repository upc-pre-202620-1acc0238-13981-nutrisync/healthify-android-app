package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AppLanguageRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import javax.inject.Inject

/** The language the app is shown in right now. */
class GetAppLanguageUseCase @Inject constructor(
    private val appLanguage: AppLanguageRepository,
) {
    operator fun invoke(): PreferredLanguage = appLanguage.current()
}

/**
 * Applies the language on the phone right away (UI only; clinical data is never translated).
 * Saving it to the account is separate ([ChangePreferredLanguageUseCase]): the app still switches language offline.
 *
 * @return `false` if that language was already active (nothing to apply).
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
