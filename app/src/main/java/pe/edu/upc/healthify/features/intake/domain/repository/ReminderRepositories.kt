package pe.edu.upc.healthify.features.intake.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings

/** PT22 · recordatorios de cada paciente en este teléfono (local; sin conexión funciona igual). */
interface ReminderSettingsRepository {

    fun observe(patientUserId: Long): Flow<ReminderSettings>

    suspend fun get(patientUserId: Long): ReminderSettings

    suspend fun save(patientUserId: Long, settings: ReminderSettings)
}

/** Programa (o cancela) los avisos locales de un tipo de recordatorio. */
interface ReminderScheduler {

    fun update(kind: ReminderKind, enabled: Boolean)
}
