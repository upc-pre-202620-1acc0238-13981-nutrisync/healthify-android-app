package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AccountRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import java.time.LocalDate
import javax.inject.Inject

/** Día en que se creó la cuenta: nada anterior a ese día pertenece a esta persona (diario PT14, «¿Cuándo comiste?»). */
class GetAccountCreatedOnUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(userId: UserId): Result<LocalDate> = accountRepository.getCreatedOn(userId)
}
