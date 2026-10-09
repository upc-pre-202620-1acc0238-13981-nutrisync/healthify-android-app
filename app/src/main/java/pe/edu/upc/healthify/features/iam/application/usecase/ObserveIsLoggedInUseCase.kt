package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

class ObserveIsLoggedInUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Boolean> = sessionRepository.isLoggedIn
}
