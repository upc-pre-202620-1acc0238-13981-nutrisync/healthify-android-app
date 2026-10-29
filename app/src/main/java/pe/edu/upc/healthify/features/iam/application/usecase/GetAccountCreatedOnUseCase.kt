package pe.edu.upc.healthify.features.iam.application.usecase

import pe.edu.upc.healthify.features.iam.domain.repository.AccountRepository
import pe.edu.upc.healthify.features.iam.domain.valueobject.UserId
import java.time.LocalDate
import javax.inject.Inject

/** Day the account was created: nothing before that day belongs to this person (the diary's meal date picker). */
class GetAccountCreatedOnUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(userId: UserId): Result<LocalDate> = accountRepository.getCreatedOn(userId)
}
