package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<SessionUser?> = sessionRepository.currentUser
}
