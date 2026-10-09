package pe.edu.upc.healthify.features.carerelationship.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import pe.edu.upc.healthify.features.carerelationship.domain.valueobject.PatientLinkStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que el dispositivo recuerda del vínculo de cada paciente que entró en él. */
interface CareLinkLocalDataSource {
    suspend fun lastKnownStatus(patientId: Long): PatientLinkStatus?
    suspend fun saveStatus(patientId: Long, status: PatientLinkStatus)
    suspend fun pendingCareLinkId(patientId: Long): Long?
    suspend fun savePendingCareLinkId(patientId: Long, careLinkId: Long)
    suspend fun clearPendingCareLinkId(patientId: Long)
}

/**
 * DataStore propio de CareRelationship. Guarda solo ids y el estado del vínculo (nada clínico); el backup está
 * deshabilitado para toda la app.
 */
@Singleton
class DataStoreCareLinkLocalDataSource @Inject constructor(
    @CareRelationshipDataStore private val dataStore: DataStore<Preferences>,
) : CareLinkLocalDataSource {

    override suspend fun lastKnownStatus(patientId: Long): PatientLinkStatus? {
        val stored = dataStore.data.first()[statusKey(patientId)] ?: return null
        return PatientLinkStatus.entries.firstOrNull { it.name == stored }
    }

    override suspend fun saveStatus(patientId: Long, status: PatientLinkStatus) {
        dataStore.edit { it[statusKey(patientId)] = status.name }
    }

    override suspend fun pendingCareLinkId(patientId: Long): Long? = dataStore.data.first()[pendingKey(patientId)]

    override suspend fun savePendingCareLinkId(patientId: Long, careLinkId: Long) {
        dataStore.edit { it[pendingKey(patientId)] = careLinkId }
    }

    override suspend fun clearPendingCareLinkId(patientId: Long) {
        dataStore.edit { it.remove(pendingKey(patientId)) }
    }

    private fun statusKey(patientId: Long) = stringPreferencesKey("link_status_$patientId")
    private fun pendingKey(patientId: Long) = longPreferencesKey("pending_care_link_$patientId")
}
