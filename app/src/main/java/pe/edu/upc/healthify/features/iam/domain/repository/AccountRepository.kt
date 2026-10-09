package pe.edu.upc.healthify.features.iam.domain.repository

import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import java.time.LocalDate

/** Datos de la cuenta del usuario con sesión que no llegan con el inicio de sesión. */
interface AccountRepository {

    /**
     * Día (en la zona del teléfono) en que se creó la cuenta: `GET /users/{userId}` → `CreatedAt`. Se lee una vez y
     * queda guardado con la sesión; sin conexión y sin lectura previa falla con `DomainError.Network`.
     */
    suspend fun getCreatedOn(userId: UserId): Result<LocalDate>
}
