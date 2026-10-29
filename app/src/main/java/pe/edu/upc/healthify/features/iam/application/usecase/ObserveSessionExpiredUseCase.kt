package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

/** "Session expired" events that lead to the session expired screen. Must have a single collector (the root graph). */
class ObserveSessionExpiredUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Unit> = sessionRepository.sessionExpired
}
