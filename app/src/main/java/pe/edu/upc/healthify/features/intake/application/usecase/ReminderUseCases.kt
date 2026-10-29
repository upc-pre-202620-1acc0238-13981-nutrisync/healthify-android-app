package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderKind
import pe.edu.upc.healthify.features.intake.domain.entity.ReminderSettings
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderScheduler
import pe.edu.upc.healthify.features.intake.domain.repository.ReminderSettingsRepository
import javax.inject.Inject

/** The patient's saved reminders. */
class ObserveReminderSettingsUseCase @Inject constructor(
    private val repository: ReminderSettingsRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<ReminderSettings> = repository.observe(patientUserId)
}

/** Turns a reminder on or off: it is saved right away and its notification is scheduled (or cancelled). */
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
