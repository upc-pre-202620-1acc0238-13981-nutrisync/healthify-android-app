package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings

/** Each patient's reminders on this phone (local; works the same offline). */
interface ReminderSettingsRepository {

    fun observe(patientUserId: Long): Flow<ReminderSettings>

    suspend fun get(patientUserId: Long): ReminderSettings

    suspend fun save(patientUserId: Long, settings: ReminderSettings)
}

/** Schedules (or cancels) the local notifications for one reminder type. */
interface ReminderScheduler {

    fun update(kind: ReminderKind, enabled: Boolean)
}
