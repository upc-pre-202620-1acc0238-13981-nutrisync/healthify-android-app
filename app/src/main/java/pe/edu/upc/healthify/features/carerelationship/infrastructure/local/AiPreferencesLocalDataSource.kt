package pe.edu.upc.healthify.features.carerelationship.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** La última lectura de `ai-preferences` de cada paciente (JSON del recurso; solo banderas, nada clínico). */
interface AiPreferencesLocalDataSource {
    suspend fun lastKnown(patientId: Long): String?
    suspend fun save(patientId: Long, json: String)
}

@Singleton
class DataStoreAiPreferencesLocalDataSource @Inject constructor(
    @CareRelationshipDataStore private val dataStore: DataStore<Preferences>,
) : AiPreferencesLocalDataSource {

    override suspend fun lastKnown(patientId: Long): String? = dataStore.data.first()[key(patientId)]

    override suspend fun save(patientId: Long, json: String) {
        dataStore.edit { it[key(patientId)] = json }
    }

    private fun key(patientId: Long) = stringPreferencesKey("ai_preferences_$patientId")
}
