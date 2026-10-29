package pe.edu.upc.healthify.features.monitoring.infrastructure.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainErrorOrNull
import pe.edu.upc.healthify.core.sync.ActiveUserIdProvider
import pe.edu.upc.healthify.features.iam.application.usecase.ObserveCurrentUserUseCase
import pe.edu.upc.healthify.features.monitoring.application.usecase.RefreshConsultationRemindersUseCase
import pe.edu.upc.healthify.features.monitoring.domain.repository.ConsultationReminderScheduler
import java.time.Duration
import java.time.Instant

/**
 * Relee las próximas consultas del usuario con sesión y reprograma sus avisos. Sin sesión los quita; sin red reintenta
 * (WorkManager espera a que vuelva).
 */
@HiltWorker
class ConsultationRemindersRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val refreshReminders: RefreshConsultationRemindersUseCase,
    private val scheduler: ConsultationReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val user = observeCurrentUser().first()
        if (user == null) {
            scheduler.cancelAll()
            return Result.success()
        }
        val result = refreshReminders(user.id.value, isPractitioner = user.isPractitioner)
        return if (result.domainErrorOrNull() == DomainError.Network) Result.retry() else Result.success()
    }
}

/**
 * Publica el aviso de una consulta si, al llegar la hora, sigue con sesión el mismo usuario para quien se programó
 * (si cerró sesión o entró otro, no se avisa).
 */
@HiltWorker
class ConsultationReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val activeUserIdProvider: ActiveUserIdProvider,
    private val notifier: ConsultationReminderNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getLong(KEY_USER_ID, -1)
        val followUpId = inputData.getLong(KEY_FOLLOW_UP_ID, -1)
        val scheduledFor = inputData.getLong(KEY_SCHEDULED_FOR, -1)
        if (userId <= 0 || followUpId <= 0 || scheduledFor <= 0) return Result.success()
        if (activeUserIdProvider.activeUserId() != userId) return Result.success()
        notifier.show(
            followUpId = followUpId,
            scheduledFor = Instant.ofEpochMilli(scheduledFor),
            lead = Duration.ofMinutes(inputData.getLong(KEY_LEAD_MINUTES, 0)),
            isPractitioner = inputData.getBoolean(KEY_IS_PRACTITIONER, false),
            patientFullName = inputData.getString(KEY_PATIENT_NAME),
        )
        return Result.success()
    }

    companion object {
        const val KEY_USER_ID = "userId"
        const val KEY_FOLLOW_UP_ID = "followUpId"
        const val KEY_SCHEDULED_FOR = "scheduledFor"
        const val KEY_LEAD_MINUTES = "leadMinutes"
        const val KEY_IS_PRACTITIONER = "isPractitioner"
        const val KEY_PATIENT_NAME = "patientName"
    }
}
