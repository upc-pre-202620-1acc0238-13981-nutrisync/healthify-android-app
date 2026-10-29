package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import java.time.LocalDate

/** Data about the signed-in user's account that does not come with sign-in. */
interface AccountRepository {

    /**
     * Day (in the phone's time zone) the account was created: `GET /users/{userId}` → `CreatedAt`. Read once and
     * stored with the session; offline and with no previous read it fails with `DomainError.Network`.
     */
    suspend fun getCreatedOn(userId: UserId): Result<LocalDate>
}
