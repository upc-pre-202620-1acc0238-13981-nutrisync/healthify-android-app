package pe.edu.upc.healthify.features.iam.domain.entity

import pe.edu.upc.healthify.features.iam.domain.valueobject.EmailAddress
import pe.edu.upc.healthify.features.iam.domain.valueobject.PersonName
import pe.edu.upc.healthify.features.iam.domain.valueobject.PreferredLanguage
import pe.edu.upc.healthify.features.iam.domain.valueobject.SessionId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserRole

/** La persona con sesión en este dispositivo. Los tokens nunca forman parte del dominio. */
data class SessionUser(
    val id: UserId,
    val email: EmailAddress,
    val name: PersonName,
    val role: UserRole,
    val preferredLanguage: PreferredLanguage,
    val sessionId: SessionId,
) {
    val isPatient: Boolean get() = role == UserRole.PATIENT
    val isPractitioner: Boolean get() = role == UserRole.PRACTITIONER
}
