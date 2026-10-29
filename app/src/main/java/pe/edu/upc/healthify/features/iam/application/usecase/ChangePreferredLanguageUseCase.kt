package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.flow.first
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import pe.edu.upc.healthify.features.iam.domain.repository.UserRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import javax.inject.Inject

/** Saves the language to the signed-in user's account. */
class ChangePreferredLanguageUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(language: PreferredLanguage): Result<Unit> {
        val user = sessionRepository.currentUser.first() ?: return domainFailure(DomainError.Unauthorized())
        if (user.preferredLanguage == language) return Result.success(Unit)
        return userRepository.changePreferredLanguage(user.id, language)
    }
}
