package pe.edu.upc.healthify.features.intake.application.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upc.healthify.features.intake.domain.entity.DiaryDay
import pe.edu.upc.healthify.features.intake.domain.entity.LoggedMealNotice
import pe.edu.upc.healthify.features.intake.domain.entity.PendingDiaryEntry
import pe.edu.upc.healthify.features.intake.domain.repository.DiaryRepository
import pe.edu.upc.healthify.features.intake.domain.valueobject.PatientId
import java.time.LocalDate
import javax.inject.Inject

/** PT14 · un día del diario (con la copia del teléfono sin conexión). */
class GetDiaryDayUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    suspend operator fun invoke(patientUserId: Long, date: LocalDate): Result<DiaryDay> =
        repository.getDiaryDay(PatientId(patientUserId), date)
}

/** PT14.O / PT19 · lo que espera en la cola del teléfono. */
class ObservePendingDiaryEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke(patientUserId: Long): Flow<List<PendingDiaryEntry>> =
        repository.observePendingEntries(PatientId(patientUserId))
}

/** PT14 / PT3 · el backend aceptó comidas de la cola: hay que volver a leer el día. */
class ObserveSyncedDiaryEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.syncedEntries
}

/** PT19 · pide enviar la cola ya (se envía sola al volver la red; esto solo la adelanta). */
class RequestDiarySyncUseCase @Inject constructor(
    private val repository: DiaryRepository,
) {
    operator fun invoke() = repository.requestSync()
}

/** PT14.1 · el Snackbar del último registro. */
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
