package pe.edu.upc.healthify.features.nutritionalcare.infrastructure.repository

import pe.edu.upc.healthify.core.network.apiCall
import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.repository.PatientRecordRepository
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.mapper.toDomain
import pe.edu.upc.healthify.features.nutritionalcare.infrastructure.remote.PatientRecordService
import javax.inject.Inject

class PatientRecordRepositoryImpl @Inject constructor(
    private val service: PatientRecordService,
) : PatientRecordRepository {

    override suspend fun getOwnRecord(patientId: PatientId): Result<PatientOwnRecord> =
        apiCall { service.getOwnRecord(patientId.value, RECORD_DAYS) }.map { it.toDomain() }

    private companion object {
        /** `PatientRecordComposer.DefaultDays`: «Mi cumplimiento» siempre son los últimos 7 días, aparte. */
        const val RECORD_DAYS = 30
    }
}
