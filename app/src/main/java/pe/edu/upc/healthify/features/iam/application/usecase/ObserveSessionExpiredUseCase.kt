package pe.edu.upc.healthify.features.iam.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.iam.domain.repository.SessionRepository
import javax.inject.Inject

/** Eventos «la sesión expiró» para navegar a S6. Debe tener un solo colector (el grafo raíz). */
class ObserveSessionExpiredUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Unit> = sessionRepository.sessionExpired
}
