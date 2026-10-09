package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderScheduler
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository
import javax.inject.Inject

/** PT22 · los recordatorios guardados del paciente. */
class ObserveReminderSettingsUseCase @Inject constructor(
    private val repository: ReminderSettingsRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<ReminderSettings> = repository.observe(patientUserId)
}

/** PT22 · enciende o apaga un recordatorio: se guarda al vuelo y se programa (o cancela) su aviso. */
class ChangeReminderUseCase @Inject constructor(
    private val repository: ReminderSettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(patientUserId: Long, kind: ReminderKind, enabled: Boolean) {
        val current = repository.get(patientUserId)
        if (current.isEnabled(kind) == enabled) return
        repository.save(patientUserId, current.with(kind, enabled))
        scheduler.update(kind, enabled)
    }
}
