package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.entity.Credentials
import pe.edu.upc.healthify.features.iam.domain.entity.NewAccount
import pe.edu.upc.healthify.features.iam.domain.entity.PasswordReset
import pe.edu.upc.healthify.features.iam.domain.entity.SessionUser
import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.NavigationShell
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetCode
import pe.edu.upc.healthify.features.iam.domain.valueobject.PasswordResetToken
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId

/** Accounts and sessions against the backend. Errors arrive as `DomainError` with the backend error `code`. */
interface AuthenticationRepository {

    /** `POST /authentication/sign-up`. Does not sign in. */
    suspend fun signUp(account: NewAccount): Result<Unit>

    /** `POST /authentication/sign-in`. On success, the session is stored on the device. */
    suspend fun signIn(credentials: Credentials): Result<SessionUser>

    /**
     * `POST /authentication/password-reset-requests`. Always `202`, whether or not the account exists: success does
     * not tell whether an email was sent.
     */
    suspend fun requestPasswordReset(email: EmailAddress): Result<Unit>

    /**
     * `POST /authentication/password-reset-verifications`. A correct code is exchanged for the token that sets the
     * password; a wrong one returns `PasswordResetCodeInvalid` and uses up an attempt.
     */
    suspend fun verifyPasswordResetCode(email: EmailAddress, code: PasswordResetCode): Result<PasswordResetToken>

    /**
     * `POST /authentication/password-resets`, with the verification token. On success, every session of the account
     * has ended; it does not sign in.
     */
    suspend fun resetPassword(reset: PasswordReset): Result<Unit>

    /** `POST /authentication/sign-out`: ends the session on the backend and deletes its refresh token. */
    suspend fun signOutRemotely(): Result<Unit>

    /**
     * `GET /sessions/{sessionId}/navigation-shell`. `null` if the session has no shell (the shell policy failed) or
     * has already ended.
     */
    suspend fun getNavigationShell(sessionId: SessionId): Result<NavigationShell?>
}
