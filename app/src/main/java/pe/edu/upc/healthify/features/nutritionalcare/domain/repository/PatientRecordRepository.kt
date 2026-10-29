package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId

/** The patient's own record. Not available offline: it is not stored on the phone. */
interface PatientRecordRepository {

    /** `GET /patients/{pid}/record?days=`. */
    suspend fun getOwnRecord(patientId: PatientId): Result<PatientOwnRecord>
}
