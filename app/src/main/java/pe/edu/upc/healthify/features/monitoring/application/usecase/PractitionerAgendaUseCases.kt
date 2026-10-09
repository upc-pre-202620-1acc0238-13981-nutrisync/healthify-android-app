package pe.edu.upc.healthify.features.monitoring.application.usecase

import pe.edu.upc.healthify.core.common.error.DomainError
import pe.edu.upc.healthify.core.common.error.domainFailure
import pe.edu.upc.healthify.features.monitoring.domain.entity.AgendaVisit
import pe.edu.upc.healthify.features.monitoring.domain.entity.CancellationReason
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpPreparation
import pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpState
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewFollowUp
import pe.edu.upc.healthify.features.monitoring.domain.entity.NewReferral
import pe.edu.upc.healthify.features.monitoring.domain.repository.PractitionerAgendaRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** PR17.0 · «Próximas consultas»: las agendadas (`state=Scheduled`) desde ahora, la más cercana primero. */
class GetUpcomingFollowUpsUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(practitionerId: Long): Result<List<AgendaVisit>> =
        repository.getAgenda(practitionerId, FollowUpState.SCHEDULED, clock.instant())
            .map { visits -> visits.sortedBy { it.scheduledFor } }
}

/**
 * PR17 «Agendar». El momento ya llega validado como futuro ([pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpMoment]);
 * si entre validar y enviar pasó la hora, se rechaza en el teléfono con el mismo código que el backend.
 */
class ScheduleFollowUpUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(followUp: NewFollowUp): Result<AgendaVisit> =
        if (!followUp.scheduledFor.isAfter(clock.instant())) {
            domainFailure(DomainError.Validation(SCHEDULED_FOR_MUST_BE_IN_FUTURE))
        } else {
            repository.schedule(followUp)
        }
}

/** PR17 en modo reprogramar (desde una consulta de PR17.0): mismo paciente, otro momento futuro. */
class RescheduleFollowUpUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(followUpId: Long, scheduledFor: Instant, preparation: FollowUpPreparation): Result<AgendaVisit> =
        if (!scheduledFor.isAfter(clock.instant())) {
            domainFailure(DomainError.Validation(SCHEDULED_FOR_MUST_BE_IN_FUTURE))
        } else {
            repository.reschedule(followUpId, scheduledFor, preparation)
        }
}

/** PR17.0 «Cancelar consulta» (no es un juicio sobre el paciente ni cierra el vínculo, MA-5). */
class CancelFollowUpUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
) {
    suspend operator fun invoke(followUpId: Long, reason: CancellationReason? = null): Result<Unit> =
        repository.cancel(followUpId, reason)
}

/** PR16 «Registrar». */
class RecordReferralUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
) {
    suspend operator fun invoke(referral: NewReferral): Result<Unit> = repository.recordReferral(referral)
}

/** Código del backend para un momento que no está en el futuro (MA-5). */
const val SCHEDULED_FOR_MUST_BE_IN_FUTURE = "ScheduledForMustBeInFuture"
