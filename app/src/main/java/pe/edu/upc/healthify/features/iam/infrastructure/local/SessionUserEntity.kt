package pe.edu.upc.healthify.features.iam.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** The user signed in on the device. A single row (fixed [slot]): there is at most one session. */
@Entity(tableName = "session_user")
data class SessionUserEntity(
    @PrimaryKey
    val slot: Int = SINGLE_SLOT,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    val email: String,
    @ColumnInfo(name = "given_names")
    val givenNames: String,
    @ColumnInfo(name = "family_names")
    val familyNames: String,
    /** `Patient` or `Practitioner`, as on the backend. */
    val role: String,
    @ColumnInfo(name = "preferred_language")
    val preferredLanguage: String,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    /** `UserResource.CreatedAt` (ISO-8601), read once with `GET /users/{id}`; `null` until then. */
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,
) {
    companion object {
        const val SINGLE_SLOT = 0
    }
}
