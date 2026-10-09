package pe.edu.upc.healthify.features.nutritionalcare.domain.repository

import pe.edu.upc.healthify.features.nutritionalcare.domain.entity.PatientOwnRecord
import pe.edu.upc.healthify.features.nutritionalcare.domain.valueobject.PatientId

/** PT20 · el expediente propio del paciente (RM-4). No disponible sin conexión: no se guarda en el teléfono. */
interface PatientRecordRepository {

    /** `GET /patients/{pid}/record?days=`. */
    suspend fun getOwnRecord(patientId: PatientId): Result<PatientOwnRecord>
}
