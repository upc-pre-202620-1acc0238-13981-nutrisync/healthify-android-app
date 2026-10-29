package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate
import javax.inject.Inject

/** One diary day (with the phone's copy when offline). */
class GetDiaryDayUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long, date: LocalDate): Result<DiaryDay> =
        repository.getDiaryDay(PatientId(patientUserId), date)
}

/** What is waiting in the phone's queue. */
class ObservePendingDiaryEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<List<PendingDiaryEntry>> =
        repository.observePendingEntries(PatientId(patientUserId))
}

/** The backend accepted queued meals: the day has to be read again. */
class ObserveSyncedDiaryEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.syncedEntries
}

/** Asks to send the queue now (it sends itself when the network returns; this only brings it forward). */
class RequestDiarySyncUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke() = repository.requestSync()
}

/** The Snackbar for the latest log. */
class ObserveLoggedMealNoticeUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke(): Flow<LoggedMealNotice?> = repository.loggedMealNotice
}

class ConsumeLoggedMealNoticeUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke() = repository.consumeLoggedMealNotice()
}
