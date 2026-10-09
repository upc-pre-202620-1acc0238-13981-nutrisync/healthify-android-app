package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId

/** Cuentas y sesiones contra el backend (§5.1 Iam). Los errores llegan como `DomainError` con el `code` de X-3. */
interface AuthenticationRepository {

    /** `POST /authentication/sign-up` (F1). No inicia sesión. */
    suspend fun signUp(account: NewAccount): Result<Unit>

    /** `POST /authentication/sign-in` (F2). Si responde, la sesión queda guardada en el dispositivo. */
    suspend fun signIn(credentials: Credentials): Result<SessionUser>

    /** `POST /authentication/sign-out` (F3): termina la sesión en el backend y borra su refresh token. */
    suspend fun signOutRemotely(): Result<Unit>

    /**
     * `GET /sessions/{sessionId}/navigation-shell`. `null` si la sesión no tiene shell (la política de F2 falló) o
     * ya está terminada.
     */
    suspend fun getNavigationShell(sessionId: SessionId): Result<NavigationShell?>
}
