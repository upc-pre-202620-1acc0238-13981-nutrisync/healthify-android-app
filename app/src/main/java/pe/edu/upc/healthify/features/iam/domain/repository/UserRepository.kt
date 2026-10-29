package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId

/** The signed-in user's account. */
interface UserRepository {

    /**
     * `PUT /users/{userId}/preferred-language`. If the backend accepts it, the user stored on the device is
     * updated too.
     */
    suspend fun changePreferredLanguage(userId: UserId, language: PreferredLanguage): Result<Unit>
}
