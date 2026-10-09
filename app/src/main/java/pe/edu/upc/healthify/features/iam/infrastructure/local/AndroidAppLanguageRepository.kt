package pe.edu.upc.healthify.features.iam.infrastructure.local

import pe.edu.upc.healthify.core.network.AppLanguageProvider
import pe.edu.upc.healthify.core.network.AppLanguageSetter
import pe.edu.upc.healthify.features.iam.domain.repository.AppLanguageRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import javax.inject.Inject

/** El idioma por app del teléfono, el mismo que viaja en `Accept-Language`. */
class AndroidAppLanguageRepository @Inject constructor(
    private val provider: AppLanguageProvider,
    private val setter: AppLanguageSetter,
) : AppLanguageRepository {

    override fun current(): PreferredLanguage = PreferredLanguage.fromCode(provider.currentLanguage())

    override fun apply(language: PreferredLanguage) = setter.apply(language.code)
}
