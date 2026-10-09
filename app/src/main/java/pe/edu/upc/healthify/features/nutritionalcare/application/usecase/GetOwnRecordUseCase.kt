package pe.edu.upc.healthify.features.nutritionalcare.application.usecase

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import javax.inject.Inject

/** PT20 · Mi expediente. */
class GetOwnRecordUseCase @Inject constructor(
    private val repository: PatientRecordRepository,
) {
    suspend operator fun invoke(patientUserId: Long): Result<PatientOwnRecord> =
        repository.getOwnRecord(PatientId(patientUserId))
}
