package pe.edu.upc.healthify.features.intake.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** DataStore de los recordatorios locales (PT22): solo banderas por paciente, nada clínico. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReminderDataStore

@Singleton
class DataStoreReminderSettingsRepository @Inject constructor(
    @ReminderDataStore private val dataStore: DataStore<Preferences>,
) : ReminderSettingsRepository {

    override fun observe(patientUserId: Long): Flow<ReminderSettings> =
        dataStore.data.map { it.toSettings(patientUserId) }.distinctUntilChanged()

    override suspend fun get(patientUserId: Long): ReminderSettings = dataStore.data.first().toSettings(patientUserId)

    override suspend fun save(patientUserId: Long, settings: ReminderSettings) {
        dataStore.edit { prefs ->
            prefs[weighInKey(patientUserId)] = settings.selfWeighInEnabled
            prefs[mealsKey(patientUserId)] = settings.mealsEnabled
        }
    }

    private fun Preferences.toSettings(patientUserId: Long) = ReminderSettings(
        selfWeighInEnabled = this[weighInKey(patientUserId)] ?: false,
        mealsEnabled = this[mealsKey(patientUserId)] ?: false,
    )

    private fun weighInKey(patientUserId: Long) = booleanPreferencesKey("self_weigh_in_$patientUserId")
    private fun mealsKey(patientUserId: Long) = booleanPreferencesKey("meals_$patientUserId")
}
