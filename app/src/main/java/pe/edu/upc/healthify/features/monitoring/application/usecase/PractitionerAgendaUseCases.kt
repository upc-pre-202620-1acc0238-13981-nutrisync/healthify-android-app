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

/** "Upcoming visits": the scheduled ones (`state=Scheduled`) from now on, nearest first. */
class GetUpcomingFollowUpsUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(practitionerId: Long): Result<List<AgendaVisit>> =
        repository.getAgenda(practitionerId, FollowUpState.SCHEDULED, clock.instant())
            .map { visits -> visits.sortedBy { it.scheduledFor } }
}

/**
 * "Schedule". The moment arrives already validated as future ([pe.edu.upc.healthify.features.monitoring.domain.entity.FollowUpMoment]);
 * if the time passed between validating and sending, the phone rejects it with the same code as the backend.
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

/** Reschedule mode (from an Agenda visit): same patient, another future moment. */
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

/** "Cancel visit" (not a judgment of the patient and does not close the care link). */
class CancelFollowUpUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
) {
    suspend operator fun invoke(followUpId: Long, reason: CancellationReason? = null): Result<Unit> =
        repository.cancel(followUpId, reason)
}

/** Record a referral. */
class RecordReferralUseCase @Inject constructor(
    private val repository: PractitionerAgendaRepository,
) {
    suspend operator fun invoke(referral: NewReferral): Result<Unit> = repository.recordReferral(referral)
}

/** Backend code for a moment that is not in the future. */
const val SCHEDULED_FOR_MUST_BE_IN_FUTURE = "ScheduledForMustBeInFuture"
