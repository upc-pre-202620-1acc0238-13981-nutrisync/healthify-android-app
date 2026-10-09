package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId

/** La cuenta del usuario con sesión. */
interface UserRepository {

    /**
     * `PUT /users/{userId}/preferred-language` (IAM-3). Si el backend lo acepta, también se actualiza el usuario
     * guardado en el dispositivo.
     */
    suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage): Result<Unit>
}
