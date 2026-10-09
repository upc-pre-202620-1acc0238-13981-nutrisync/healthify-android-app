package pe.edu.upc.healthify.features.iam.infrastructure.repository

import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.iam.domain.repository.UserRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.infrastructure.local.SessionLocalDataSource
import pe.edu.upc.healthify.features.iam.infrastructure.remote.SessionService
import pe.edu.upc.healthify.features.iam.infrastructure.remote.dto.ChangePreferredLanguageRequestDto
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val sessionService: SessionService,
    private val localDataSource: SessionLocalDataSource,
) : UserRepository {

    override suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage): Result<Unit> {
        val result = apiCall {
            sessionService.changePreferredLanguage(userId.value, ChangePreferredLanguageRequestDto(language.code))
        }
        if (result.isSuccess) localDataSource.updatePreferredLanguage(language)
        return result
    }
}
