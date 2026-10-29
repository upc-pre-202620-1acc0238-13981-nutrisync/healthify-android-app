package pe.edu.upc.healthify.features.monitoring.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pe.edu.upc.healthify.features.monitoring.presentation.screen.RecordReferralScreen
import pe.edu.upc.healthify.features.monitoring.presentation.screen.ScheduleFollowUpScreen
import java.time.Instant

/**
 * Schedule a visit. From the patient profile it arrives with a fixed patient ([patientId] > 0); from the Agenda
 * without one (picked from the roster). With [followUpId] > 0 it reschedules that visit (fixed patient, date and
 * preparation prefilled: [scheduledForEpochSecond] and [preparation] as comma-separated codes).
 */
@Serializable
data class ScheduleFollowUpRoute(
    val patientId: Long = 0,
    val patientName: String = "",
    val followUpId: Long = 0,
    val scheduledForEpochSecond: Long = 0,
    val preparation: String = "",
) {
    companion object {
        const val ARG_PATIENT_ID = "patientId"
        const val ARG_PATIENT_NAME = "patientName"
        const val ARG_FOLLOW_UP_ID = "followUpId"
        const val ARG_SCHEDULED_FOR = "scheduledForEpochSecond"
        const val ARG_PREPARATION = "preparation"
    }
}

/** Record a referral (from the patient profile's "Record referral"). */
@Serializable
data class RecordReferralRoute(val patientId: Long, val patientName: String) {
    companion object {
        const val ARG_PATIENT_ID = "patientId"
    }
}

data class PractitionerAgendaNavigationCallbacks(
    val onBack: () -> Unit,
    /** Scheduled or rescheduled: returns to where it was opened, with the Snackbar (Agenda or patient profile). */
    val onScheduled: (scheduledFor: Instant, rescheduled: Boolean) -> Unit,
    /** Referral recorded: returns to the patient profile with the "Referral … recorded" Snackbar. */
    val onReferralRecorded: (specialty: String) -> Unit,
)

/** Schedule visit and record referral in the root graph. */
fun NavGraphBuilder.practitionerAgendaDestinations(callbacks: PractitionerAgendaNavigationCallbacks) {
    composable<ScheduleFollowUpRoute> {
        ScheduleFollowUpScreen(onBack = callbacks.onBack, onScheduled = callbacks.onScheduled)
    }
    composable<RecordReferralRoute> {
        RecordReferralScreen(onBack = callbacks.onBack, onRecorded = callbacks.onReferralRecorded)
    }
}
